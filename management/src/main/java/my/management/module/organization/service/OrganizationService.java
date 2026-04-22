package my.management.module.organization.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.employee.mapper.DepartmentMapper;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Department;
import my.management.module.organization.mapper.OrganizationMapper;
import my.management.module.organization.model.dto.OrganizationDepartmentSaveRequest;
import my.management.module.organization.model.vo.OrganizationDepartmentVO;
import my.management.module.organization.model.vo.OrganizationEmployeeVO;
import my.management.module.organization.model.vo.OrganizationOverviewVO;
import my.management.module.organization.model.vo.OrganizationStatsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 组织架构服务，围绕部门层级维护和部门员工查看进行业务编排。
 */
@Service
public class OrganizationService {

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    public OrganizationOverviewVO overview() {
        List<Department> departments = listTenantDepartments();
        Map<String, Long> employeeCountMap = buildEmployeeCountMap();
        Map<Long, Long> positionCountMap = buildPositionCountMap();

        List<OrganizationDepartmentVO> flatNodes = departments.stream()
                .map(department -> toDepartmentVO(department, employeeCountMap, positionCountMap))
                .toList();

        OrganizationOverviewVO overview = new OrganizationOverviewVO();
        overview.setDepartments(buildTree(flatNodes));
        overview.setStats(buildStats(flatNodes));
        return overview;
    }

    public List<OrganizationEmployeeVO> employees(Long departmentId) {
        Department department = requireDepartment(departmentId);
        return organizationMapper.selectEmployeesByDepartment(TenantPermissionContext.getTenantCode(), department.getDeptName());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(OrganizationDepartmentSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long parentId = normalizeParentId(request.getParentId());
        validateParent(parentId, request.getId());

        Department department;
        String oldName = null;
        if (request.getId() == null) {
            department = new Department();
            department.setTenantCode(tenantCode);
            department.setDeptCode(resolveDeptCode(request.getDeptCode()));
            department.setIsDeleted(0);
        } else {
            department = requireDepartment(request.getId());
            oldName = department.getDeptName();
            if (StringUtils.hasText(request.getDeptCode())) {
                department.setDeptCode(request.getDeptCode().trim());
            }
        }

        department.setParentId(parentId);
        department.setDeptName(request.getDeptName().trim());
        department.setLeaderName(blankToNull(request.getLeaderName()));
        department.setSortNo(request.getSortNo() == null ? 99 : request.getSortNo());
        department.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        ensureNameUnique(department);

        if (department.getId() == null) {
            departmentMapper.insert(department);
        } else {
            departmentMapper.updateById(department);
            if (StringUtils.hasText(oldName) && !Objects.equals(oldName, department.getDeptName())) {
                organizationMapper.updateEmployeeDepartmentName(tenantCode, oldName, department.getDeptName());
            }
        }
        return department.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Department department = requireDepartment(id);
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id)
                .eq(Department::getIsDeleted, 0));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("请先删除或调整下级部门后再删除");
        }
        Long employeeCount = buildEmployeeCountMap().getOrDefault(department.getDeptName(), 0L);
        if (employeeCount > 0) {
            throw new BusinessException("该部门下仍有员工，请先调整员工部门后再删除");
        }
        department.setIsDeleted(1);
        departmentMapper.updateById(department);
    }

    private List<Department> listTenantDepartments() {
        return departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                .eq(Department::getIsDeleted, 0)
                .orderByAsc(Department::getSortNo)
                .orderByAsc(Department::getId));
    }

    private OrganizationDepartmentVO toDepartmentVO(Department department, Map<String, Long> employeeCountMap, Map<Long, Long> positionCountMap) {
        OrganizationDepartmentVO vo = new OrganizationDepartmentVO();
        BeanUtils.copyProperties(department, vo);
        vo.setEmployeeCount(employeeCountMap.getOrDefault(department.getDeptName(), 0L));
        vo.setPositionCount(positionCountMap.getOrDefault(department.getId(), 0L));
        return vo;
    }

    private List<OrganizationDepartmentVO> buildTree(List<OrganizationDepartmentVO> flatNodes) {
        Map<Long, OrganizationDepartmentVO> nodeMap = new LinkedHashMap<>();
        flatNodes.forEach(node -> nodeMap.put(node.getId(), node));

        List<OrganizationDepartmentVO> roots = new ArrayList<>();
        for (OrganizationDepartmentVO node : flatNodes) {
            if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<OrganizationDepartmentVO> nodes) {
        nodes.sort(Comparator.comparing((OrganizationDepartmentVO item) -> item.getSortNo() == null ? 99 : item.getSortNo())
                .thenComparing(OrganizationDepartmentVO::getId));
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    private OrganizationStatsVO buildStats(List<OrganizationDepartmentVO> nodes) {
        OrganizationStatsVO stats = new OrganizationStatsVO();
        stats.setDepartmentCount((long) nodes.size());
        stats.setEmployeeCount(employeeMapper.countAvailableEmployees(TenantPermissionContext.getTenantCode()));
        stats.setEnabledDepartmentCount(nodes.stream().filter(node -> Integer.valueOf(1).equals(node.getStatus())).count());
        stats.setEmptyDepartmentCount(nodes.stream().filter(node -> node.getEmployeeCount() == null || node.getEmployeeCount() == 0).count());
        return stats;
    }

    private Map<String, Long> buildEmployeeCountMap() {
        Map<String, Long> result = new LinkedHashMap<>();
        organizationMapper.selectDepartmentEmployeeCounts(TenantPermissionContext.getTenantCode()).forEach(row -> {
            String departmentName = row.get("departmentName") == null ? null : String.valueOf(row.get("departmentName"));
            Long count = toLong(row.get("employeeCount"));
            if (StringUtils.hasText(departmentName)) {
                result.put(departmentName, count == null ? 0L : count);
            }
        });
        return result;
    }

    private Map<Long, Long> buildPositionCountMap() {
        Map<Long, Long> result = new LinkedHashMap<>();
        organizationMapper.selectDepartmentPositionCounts(TenantPermissionContext.getTenantCode()).forEach(row -> {
            Long departmentId = toLong(row.get("departmentId"));
            Long count = toLong(row.get("positionCount"));
            if (departmentId != null) {
                result.put(departmentId, count == null ? 0L : count);
            }
        });
        return result;
    }

    private Department requireDepartment(Long id) {
        Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getId, id)
                .eq(Department::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        return department;
    }

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(parentId, currentId)) {
            throw new BusinessException("上级部门不能选择自身");
        }
        Department parent = requireDepartment(parentId);
        Long cursor = parent.getParentId();
        while (cursor != null) {
            if (Objects.equals(cursor, currentId)) {
                throw new BusinessException("不能把下级部门设置为上级部门");
            }
            Department next = requireDepartment(cursor);
            cursor = next.getParentId();
        }
    }

    private void ensureNameUnique(Department department) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptName, department.getDeptName())
                .eq(Department::getIsDeleted, 0);
        if (department.getId() != null) {
            wrapper.ne(Department::getId, department.getId());
        }
        Long count = departmentMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("部门名称已存在");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private String resolveDeptCode(String deptCode) {
        if (StringUtils.hasText(deptCode)) {
            return deptCode.trim();
        }
        return codeGeneratorUtil.generateCode("DPT", 4);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
