package my.hive.domain.organization.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.enums.CommonStatusEnum;
import my.hive.shared.enums.DeleteFlagEnum;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.domain.employee.mapper.DepartmentMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.mapper.PositionMapper;
import my.hive.domain.employee.model.entity.Department;
import my.hive.domain.employee.model.entity.Position;
import my.hive.domain.organization.mapper.OrganizationMapper;
import my.hive.domain.organization.model.dto.OrganizationDepartmentSaveRequest;
import my.hive.domain.organization.model.dto.OrganizationPositionSaveRequest;
import my.hive.domain.organization.model.vo.OrganizationDepartmentVO;
import my.hive.domain.organization.model.vo.OrganizationEmployeeVO;
import my.hive.domain.organization.model.vo.OrganizationJoinCodeVO;
import my.hive.domain.organization.model.vo.OrganizationOverviewVO;
import my.hive.domain.organization.model.vo.OrganizationPositionVO;
import my.hive.domain.organization.model.vo.OrganizationStatsVO;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 组织架构服务，围绕部门层级维护和部门员工查看进行业务编排。
 */
@Service
public class OrganizationService {

    private static final long JOIN_CODE_EXPIRE_SECONDS = 15L * 60L;
    private static final String JOIN_CODE_KEY_PART = "organization-join-code";
    private static final char[] JOIN_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private PrivacyProtectionUtil privacyProtectionUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

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
        List<OrganizationEmployeeVO> employees = organizationMapper.selectEmployeesByDepartment(TenantPermissionContext.getTenantCode(), department.getDeptName());
        employees.forEach(item -> item.setPhone(privacyProtectionUtil.maskPhone(item.getPhone())));
        return employees;
    }

    public List<OrganizationPositionVO> positions(Long departmentId) {
        requireDepartment(departmentId);
        return organizationMapper.selectPositions(TenantPermissionContext.getTenantCode(), departmentId);
    }

    public OrganizationJoinCodeVO createJoinCode() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException(401, "当前登录组织异常，请重新登录");
        }

        String code = generateJoinCode();
        String key = redisKeyBuilder.cache("auth", JOIN_CODE_KEY_PART, code);
        stringRedisTemplate.opsForValue().set(key, tenantCode, JOIN_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        OrganizationJoinCodeVO vo = new OrganizationJoinCodeVO();
        vo.setOrganizationCode(code);
        vo.setExpiresInSeconds(JOIN_CODE_EXPIRE_SECONDS);
        vo.setExpireAt(System.currentTimeMillis() / 1000 + JOIN_CODE_EXPIRE_SECONDS);
        return vo;
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
            department.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
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
        Long positionCount = positionMapper.selectCount(new LambdaQueryWrapper<Position>()
                .eq(Position::getDepartmentId, id)
                .eq(Position::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        if (positionCount != null && positionCount > 0) {
            throw new BusinessException("该部门下仍有职位，请先删除职位后再删除部门");
        }
        department.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
        departmentMapper.updateById(department);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long savePosition(OrganizationPositionSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Department department = requireDepartment(request.getDepartmentId());
        Position position;
        String oldName = null;
        Department oldDepartment = null;
        if (request.getId() == null) {
            position = new Position();
            position.setTenantCode(tenantCode);
            position.setPositionCode(resolvePositionCode(request.getPositionCode()));
            position.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        } else {
            position = requirePosition(request.getId());
            oldName = position.getPositionName();
            oldDepartment = requireDepartment(position.getDepartmentId());
            if (!Objects.equals(position.getDepartmentId(), department.getId())
                    && countEmployeesByPosition(oldDepartment, position) > 0) {
                throw new BusinessException("该职位仍有员工，不能移动到其他部门");
            }
            if (StringUtils.hasText(request.getPositionCode())) {
                position.setPositionCode(request.getPositionCode().trim());
            }
        }

        position.setDepartmentId(department.getId());
        position.setPositionName(request.getPositionName().trim());
        position.setSortNo(request.getSortNo() == null ? 99 : request.getSortNo());
        position.setStatus(request.getStatus() == null ? CommonStatusEnum.ENABLED.getCode() : request.getStatus());
        ensurePositionNameUnique(position);

        if (position.getId() == null) {
            positionMapper.insert(position);
        } else {
            positionMapper.updateById(position);
            if (oldDepartment != null && Objects.equals(oldDepartment.getId(), department.getId())
                    && StringUtils.hasText(oldName) && !Objects.equals(oldName, position.getPositionName())) {
                organizationMapper.updateEmployeePositionName(
                        tenantCode, department.getDeptName(), oldName, position.getPositionName());
            }
        }
        return position.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        Position position = requirePosition(id);
        Department department = requireDepartment(position.getDepartmentId());
        if (countEmployeesByPosition(department, position) > 0) {
            throw new BusinessException("该职位仍有员工，请先调整员工职位后再删除");
        }
        position.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
        positionMapper.updateById(position);
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
        stats.setEnabledDepartmentCount(nodes.stream().filter(node -> CommonStatusEnum.isEnabled(node.getStatus())).count());
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

    private Position requirePosition(Long id) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getId, id)
                .eq(Position::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (position == null) {
            throw new BusinessException("职位不存在");
        }
        return position;
    }

    private long countEmployeesByPosition(Department department, Position position) {
        Long count = organizationMapper.countEmployeesByPosition(
                TenantPermissionContext.getTenantCode(), department.getDeptName(), position.getPositionName());
        return count == null ? 0L : count;
    }

    private void ensurePositionNameUnique(Position position) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<Position>()
                .eq(Position::getDepartmentId, position.getDepartmentId())
                .eq(Position::getPositionName, position.getPositionName())
                .eq(Position::getIsDeleted, DeleteFlagEnum.NORMAL.getCode());
        if (position.getId() != null) {
            wrapper.ne(Position::getId, position.getId());
        }
        Long count = positionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该部门下已存在同名职位");
        }
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

    private String resolvePositionCode(String positionCode) {
        if (StringUtils.hasText(positionCode)) {
            return positionCode.trim();
        }
        return codeGeneratorUtil.generateCode("POS", 4);
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

    private String generateJoinCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder builder = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                builder.append(JOIN_CODE_CHARS[SECURE_RANDOM.nextInt(JOIN_CODE_CHARS.length)]);
            }
            String code = builder.toString();
            String key = redisKeyBuilder.cache("auth", JOIN_CODE_KEY_PART, code);
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
                return code;
            }
        }
        throw new BusinessException(500, "组织码生成失败，请稍后重试");
    }
}
