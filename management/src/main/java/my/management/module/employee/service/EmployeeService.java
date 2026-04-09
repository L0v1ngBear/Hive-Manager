package my.management.module.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.employee.mapper.DepartmentMapper;
import my.management.module.employee.mapper.EmployeeChangeLogMapper;
import my.management.module.employee.mapper.EmployeeExtMapper;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.mapper.PositionMapper;
import my.management.module.employee.model.dto.EmployeeBatchUpdateRequest;
import my.management.module.employee.model.dto.EmployeeCreateRequest;
import my.management.module.employee.model.dto.EmployeePageQuery;
import my.management.module.employee.model.dto.EmployeeStatusChangeRequest;
import my.management.module.employee.model.dto.EmployeeUpdateRequest;
import my.management.module.employee.model.entity.Department;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.entity.EmployeeChangeLog;
import my.management.module.employee.model.entity.EmployeeExt;
import my.management.module.employee.model.entity.Position;
import my.management.module.employee.model.vo.DepartmentOptionVO;
import my.management.module.employee.model.vo.EmployeeDetailVO;
import my.management.module.employee.model.vo.EmployeeFormOptionsVO;
import my.management.module.employee.model.vo.EmployeeLeaderOptionVO;
import my.management.module.employee.model.vo.EmployeePageVO;
import my.management.module.employee.model.vo.EmployeeStatsVO;
import my.management.module.employee.model.vo.OptionVO;
import my.management.module.employee.model.vo.PositionOptionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {

    private static final int STATUS_RESIGNED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_PROBATION = 2;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private EmployeeExtMapper employeeExtMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private EmployeeChangeLogMapper employeeChangeLogMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private ObjectMapper objectMapper;

    public Page<EmployeePageVO> page(EmployeePageQuery query) {
        Page<EmployeePageVO> page = new Page<>(query.getPage(), query.getSize());
        Page<EmployeePageVO> result = employeeMapper.selectEmployeePage(
                page,
                query.getKeyword(),
                query.getDepartmentId(),
                query.getStatus(),
                query.getEmployeeType(),
                query.getEntryDateStart(),
                query.getEntryDateEnd());
        result.getRecords().forEach(this::fillStatusLabel);
        return result;
    }

    public EmployeeStatsVO stats() {
        EmployeeStatsVO vo = new EmployeeStatsVO();
        Long totalEmployees = nvl(employeeMapper.countAvailableEmployees());
        Long attendanceUsers = nvl(employeeMapper.countTodayAttendanceUsers());
        vo.setTotalEmployees(totalEmployees);
        vo.setDepartmentCount(nvl(employeeMapper.countDistinctDepartments()));
        vo.setPendingOnboardCount(nvl(employeeMapper.countPendingOnboard()));
        vo.setTodayAttendanceRate(totalEmployees == 0 ? 0D : Math.round(attendanceUsers * 10000.0 / totalEmployees) / 100.0);
        return vo;
    }

    public EmployeeDetailVO detail(Long id) {
        EmployeeDetailVO detail = employeeMapper.selectEmployeeDetail(id);
        if (detail == null) {
            throw new BusinessException("employee not found");
        }
        fillStatusLabel(detail);
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(@Valid EmployeeCreateRequest request) {
        Department department = requireDepartment(request.getDepartmentId());
        Position position = requirePosition(request.getPositionId());
        validateLeader(request.getLeaderId());

        Employee employee = new Employee();
        employee.setTenantCode(TenantPermissionContext.getTenantCode());
        employee.setName(request.getName());
        employee.setPhone(request.getPhone());
        employee.setDepartmentName(department.getDeptName());
        employee.setPosition(position.getPositionName());
        employee.setManagerId(request.getLeaderId());
        employee.setStatus(request.getStatus());
        employeeMapper.insert(employee);

        EmployeeExt ext = new EmployeeExt();
        ext.setUserId(employee.getId());
        ext.setTenantCode(TenantPermissionContext.getTenantCode());
        ext.setEmpNo(codeGeneratorUtil.generateEmployeeNo());
        ext.setEmail(request.getEmail());
        ext.setEmployeeType(request.getEmployeeType());
        ext.setEntryDate(request.getEntryDate());
        ext.setAvatarUrl(request.getAvatarUrl());
        ext.setRemark(request.getRemark());
        ext.setIsDeleted(0);
        employeeExtMapper.insert(ext);

        insertChangeLog(employee.getId(), "CREATE", null, Map.of(
                "name", employee.getName(),
                "phone", employee.getPhone(),
                "departmentName", employee.getDepartmentName(),
                "position", employee.getPosition(),
                "status", employee.getStatus(),
                "empNo", ext.getEmpNo()
        ));
        return employee.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(@Valid EmployeeUpdateRequest request) {
        Employee employee = requireEmployee(request.getId());
        EmployeeDetailVO before = detail(request.getId());
        Department department = requireDepartment(request.getDepartmentId());
        Position position = requirePosition(request.getPositionId());
        validateLeader(request.getLeaderId());

        employee.setName(request.getName());
        employee.setPhone(request.getPhone());
        employee.setDepartmentName(department.getDeptName());
        employee.setPosition(position.getPositionName());
        employee.setManagerId(request.getLeaderId());
        employee.setStatus(request.getStatus());
        employeeMapper.updateById(employee);

        EmployeeExt ext = getOrCreateExt(employee.getId());
        if (!StringUtils.hasText(ext.getEmpNo())) {
            ext.setEmpNo(codeGeneratorUtil.generateEmployeeNo());
        }
        ext.setEmail(request.getEmail());
        ext.setEmployeeType(request.getEmployeeType());
        ext.setEntryDate(request.getEntryDate());
        ext.setAvatarUrl(request.getAvatarUrl());
        ext.setRemark(request.getRemark());
        saveOrUpdateExt(ext);

        insertChangeLog(employee.getId(), "UPDATE", before, detail(employee.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(@Valid EmployeeStatusChangeRequest request) {
        Employee employee = requireEmployee(request.getId());
        Integer beforeStatus = employee.getStatus();
        employee.setStatus(request.getStatus());
        employeeMapper.updateById(employee);
        if (StringUtils.hasText(request.getRemark())) {
            EmployeeExt ext = getOrCreateExt(employee.getId());
            ext.setRemark(request.getRemark());
            saveOrUpdateExt(ext);
        }
        insertChangeLog(employee.getId(), "CHANGE_STATUS", Map.of("status", beforeStatus), Map.of("status", request.getStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(@Valid EmployeeBatchUpdateRequest request) {
        if (request.getDepartmentId() == null && request.getPositionId() == null && request.getLeaderId() == null
                && request.getStatus() == null && !StringUtils.hasText(request.getRemark())) {
            throw new BusinessException("at least one field is required for batch update");
        }

        Department department = request.getDepartmentId() == null ? null : requireDepartment(request.getDepartmentId());
        Position position = request.getPositionId() == null ? null : requirePosition(request.getPositionId());
        validateLeader(request.getLeaderId());

        for (Long id : request.getIds()) {
            Employee employee = requireEmployee(id);
            EmployeeDetailVO before = detail(id);
            if (department != null) {
                employee.setDepartmentName(department.getDeptName());
            }
            if (position != null) {
                employee.setPosition(position.getPositionName());
            }
            if (request.getLeaderId() != null) {
                employee.setManagerId(request.getLeaderId());
            }
            if (request.getStatus() != null) {
                employee.setStatus(request.getStatus());
            }
            employeeMapper.updateById(employee);

            if (StringUtils.hasText(request.getRemark())) {
                EmployeeExt ext = getOrCreateExt(id);
                ext.setRemark(request.getRemark());
                saveOrUpdateExt(ext);
            }
            insertChangeLog(id, "BATCH_UPDATE", before, detail(id));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireEmployee(id);
        EmployeeExt ext = getOrCreateExt(id);
        ext.setIsDeleted(1);
        saveOrUpdateExt(ext);
        insertChangeLog(id, "DELETE", Map.of("isDeleted", 0), Map.of("isDeleted", 1));
    }

    public List<EmployeeLeaderOptionVO> searchLeaders(String keyword, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return employeeMapper.searchLeaders(keyword, safeLimit);
    }

    public EmployeeFormOptionsVO initFormOptions() {
        EmployeeFormOptionsVO vo = new EmployeeFormOptionsVO();
        vo.setDepartments(departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                        .eq(Department::getStatus, 1)
                        .orderByAsc(Department::getSortNo))
                .stream()
                .map(this::toDepartmentOption)
                .toList());
        vo.setPositions(positionMapper.selectList(new LambdaQueryWrapper<Position>()
                        .eq(Position::getStatus, 1)
                        .orderByAsc(Position::getSortNo))
                .stream()
                .map(this::toPositionOption)
                .toList());
        vo.setEmployeeTypes(List.of(
                new OptionVO("Full Time", "FULL_TIME"),
                new OptionVO("Contract", "CONTRACT"),
                new OptionVO("Probation", "PROBATION")
        ));
        vo.setEmploymentStatuses(List.of(
                new OptionVO("Resigned", String.valueOf(STATUS_RESIGNED)),
                new OptionVO("Active", String.valueOf(STATUS_ACTIVE)),
                new OptionVO("Probation", String.valueOf(STATUS_PROBATION))
        ));
        return vo;
    }

    public List<EmployeePageVO> export(EmployeePageQuery query) {
        List<EmployeePageVO> list = employeeMapper.selectEmployeeExport(
                query.getKeyword(),
                query.getDepartmentId(),
                query.getStatus(),
                query.getEmployeeType(),
                query.getEntryDateStart(),
                query.getEntryDateEnd());
        list.forEach(this::fillStatusLabel);
        return list;
    }

    private Employee requireEmployee(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("employee not found");
        }
        EmployeeExt ext = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>().eq(EmployeeExt::getUserId, id));
        if (ext != null && Integer.valueOf(1).equals(ext.getIsDeleted())) {
            throw new BusinessException("employee has been deleted");
        }
        return employee;
    }

    private Department requireDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null || Integer.valueOf(1).equals(department.getIsDeleted()) || !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException("department is invalid");
        }
        return department;
    }

    private Position requirePosition(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null || Integer.valueOf(1).equals(position.getIsDeleted()) || !Integer.valueOf(1).equals(position.getStatus())) {
            throw new BusinessException("position is invalid");
        }
        return position;
    }

    private void validateLeader(Long leaderId) {
        if (leaderId == null) {
            return;
        }
        Employee leader = employeeMapper.selectById(leaderId);
        if (leader == null) {
            throw new BusinessException("leader not found");
        }
    }

    private EmployeeExt getOrCreateExt(Long userId) {
        EmployeeExt ext = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>().eq(EmployeeExt::getUserId, userId));
        if (ext != null) {
            return ext;
        }
        EmployeeExt created = new EmployeeExt();
        created.setUserId(userId);
        created.setTenantCode(TenantPermissionContext.getTenantCode());
        created.setIsDeleted(0);
        return created;
    }

    private void saveOrUpdateExt(EmployeeExt ext) {
        if (ext.getId() == null) {
            employeeExtMapper.insert(ext);
        } else {
            employeeExtMapper.updateById(ext);
        }
    }

    private void fillStatusLabel(EmployeePageVO vo) {
        if (vo == null) {
            return;
        }
        vo.setStatusLabel(statusLabel(vo.getStatus()));
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case STATUS_RESIGNED -> "RESIGNED";
            case STATUS_ACTIVE -> "ACTIVE";
            case STATUS_PROBATION -> "PROBATION";
            default -> "UNKNOWN";
        };
    }

    private void insertChangeLog(Long employeeId, String changeType, Object before, Object after) {
        EmployeeChangeLog log = new EmployeeChangeLog();
        log.setTenantCode(TenantPermissionContext.getTenantCode());
        log.setEmployeeId(employeeId);
        log.setChangeType(changeType);
        log.setBeforeJson(writeJson(before));
        log.setAfterJson(writeJson(after));
        log.setOperatorUserId(TenantPermissionContext.getUserId());
        employeeChangeLogMapper.insert(log);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("failed to serialize employee change log");
        }
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private DepartmentOptionVO toDepartmentOption(Department department) {
        DepartmentOptionVO vo = new DepartmentOptionVO();
        vo.setId(department.getId());
        vo.setName(department.getDeptName());
        vo.setCode(department.getDeptCode());
        return vo;
    }

    private PositionOptionVO toPositionOption(Position position) {
        PositionOptionVO vo = new PositionOptionVO();
        vo.setId(position.getId());
        vo.setName(position.getPositionName());
        vo.setCode(position.getPositionCode());
        vo.setDepartmentId(position.getDepartmentId());
        return vo;
    }
}
