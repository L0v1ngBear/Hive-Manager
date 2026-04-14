package my.management.module.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.common.utils.ExcelUtil;
import my.management.common.utils.EncryptUtil;
import my.management.common.vo.ImportResultVO;
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
import my.management.module.employee.model.vo.RoleOptionVO;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysUserRole;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EmployeeService {

    private static final int STATUS_RESIGNED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_PROBATION = 2;
    private static final String DEFAULT_PASSWORD = "123456";

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

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private ExcelUtil excelUtil;

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
        result.getRecords().forEach(this::fillViewFields);
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
        fillViewFields(detail);
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
        employee.setLoginName(request.getPhone());
        employee.setPhone(request.getPhone());
        employee.setPassword(encryptUtil.encode(DEFAULT_PASSWORD));
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
        syncUserRoles(employee.getId(), request.getRoleIds());

        insertChangeLog(employee.getId(), "CREATE", null, Map.of(
                "name", employee.getName(),
                "loginName", employee.getLoginName(),
                "phone", employee.getPhone(),
                "departmentName", employee.getDepartmentName(),
                "position", employee.getPosition(),
                "status", employee.getStatus(),
                "empNo", ext.getEmpNo(),
                "defaultPassword", DEFAULT_PASSWORD
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

        boolean syncLoginName = employee.getLoginName() == null || employee.getLoginName().isBlank() || employee.getLoginName().equals(employee.getPhone());
        employee.setName(request.getName());
        employee.setPhone(request.getPhone());
        if (syncLoginName) {
            employee.setLoginName(request.getPhone());
        }
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
        syncUserRoles(employee.getId(), request.getRoleIds());

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
        vo.setRoles(sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(SysRole::getIsDeleted, 0)
                        .orderByDesc(SysRole::getIsSystem)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(this::toRoleOption)
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
        list.forEach(this::fillViewFields);
        return list;
    }

    public void exportExcel(EmployeePageQuery query, HttpServletResponse response) {
        List<String> headers = List.of("姓名", "工号", "部门", "职位", "邮箱", "电话", "状态", "员工类型", "直属领导", "角色", "入职日期");
        List<List<String>> rows = export(query).stream()
                .map(item -> List.of(
                        excelUtil.stringify(item.getName()),
                        excelUtil.stringify(item.getEmpNo()),
                        excelUtil.stringify(item.getDepartmentName()),
                        excelUtil.stringify(item.getPositionName()),
                        excelUtil.stringify(item.getEmail()),
                        excelUtil.stringify(item.getPhone()),
                        statusLabelCn(item.getStatus()),
                        employeeTypeLabel(item.getEmployeeType()),
                        excelUtil.stringify(item.getLeaderName()),
                        String.join("、", item.getRoleNames() == null ? Collections.emptyList() : item.getRoleNames()),
                        excelUtil.stringify(item.getEntryDate())
                ))
                .toList();
        excelUtil.writeToResponse(response,
                excelUtil.createWorkbook("员工列表", headers, rows),
                "员工列表.xlsx");
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        List<String> headers = List.of("姓名", "手机号", "部门", "职位", "状态", "员工类型", "入职日期", "邮箱", "直属领导手机号", "角色名称", "备注");
        List<List<String>> examples = List.of(
                List.of("张三", "13900030001", "仓储部", "仓库专员", "在职", "全职", LocalDate.now().toString(), "zhangsan@example.com", "13900010002", "普通员工", "示例数据"),
                List.of("李四", "13900030002", "业务部", "销售专员", "试用", "试用期", LocalDate.now().toString(), "lisi@example.com", "13900010004", "业务测试管理员,普通员工", "")
        );
        List<String> notes = List.of(
                "仅支持 .xlsx 文件导入。",
                "必填列：姓名、手机号、部门、职位。",
                "状态支持：在职、试用、离职。为空时默认在职。",
                "员工类型支持：全职、合同工、试用期。为空时默认全职。",
                "入职日期格式：yyyy-MM-dd。为空时默认当天。",
                "直属领导手机号可不填；填写后必须是当前租户已存在员工手机号。",
                "角色名称可填多个，使用英文逗号、中文逗号或顿号分隔；角色必须已存在。",
                "若部门或职位不存在，系统会自动创建启用中的部门和职位。"
        );
        excelUtil.writeToResponse(response,
                excelUtil.createTemplateWorkbook("员工导入模板", headers, examples, notes),
                "员工导入模板.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importEmployees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择要导入的 Excel 文件");
        }
        ImportResultVO result = new ImportResultVO();
        try (var inputStream = file.getInputStream(); var workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, 11)) {
                    continue;
                }
                result.setTotalCount(result.getTotalCount() + 1);
                try {
                    create(buildImportRequest(row));
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    if (result.getFailMessages().size() < 20) {
                        result.getFailMessages().add("第 " + (i + 1) + " 行：" + ex.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取员工导入文件失败");
        }
        return result;
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

    private void fillViewFields(EmployeePageVO vo) {
        if (vo == null) {
            return;
        }
        vo.setStatusLabel(statusLabel(vo.getStatus()));
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserIdAndTenantCode(vo.getId(), TenantPermissionContext.getTenantCode());
        List<String> roleNames = sysUserRoleMapper.selectRoleNamesByUserIdAndTenantCode(vo.getId(), TenantPermissionContext.getTenantCode());
        vo.setRoleIds(roleIds == null ? Collections.emptyList() : roleIds);
        vo.setRoleNames(roleNames == null ? Collections.emptyList() : roleNames);
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

    private RoleOptionVO toRoleOption(SysRole role) {
        RoleOptionVO vo = new RoleOptionVO();
        vo.setId(role.getId());
        vo.setName(role.getRoleName());
        return vo;
    }

    private void syncUserRoles(Long userId, List<Long> roleIds) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<SysUserRole> existed = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantCode, tenantCode)
                .eq(SysUserRole::getIsDeleted, 0));
        for (SysUserRole item : existed) {
            item.setIsDeleted(1);
            sysUserRoleMapper.updateById(item);
        }

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getIsDeleted, 0)
                .in(SysRole::getId, roleIds));
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("存在无效角色，无法分配");
        }

        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setTenantCode(tenantCode);
            userRole.setRoleId(roleId);
            userRole.setIsDeleted(0);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private EmployeeCreateRequest buildImportRequest(Row row) {
        String name = excelUtil.readString(row.getCell(0));
        String phone = excelUtil.readString(row.getCell(1));
        String departmentName = excelUtil.readString(row.getCell(2));
        String positionName = excelUtil.readString(row.getCell(3));
        if (!StringUtils.hasText(name) || !StringUtils.hasText(phone) || !StringUtils.hasText(departmentName) || !StringUtils.hasText(positionName)) {
            throw new BusinessException("姓名、手机号、部门、职位不能为空");
        }
        ensurePhoneNotExists(phone);

        Department department = getOrCreateDepartment(departmentName);
        Position position = getOrCreatePosition(positionName, department.getId());

        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setDepartmentId(department.getId());
        request.setPositionId(position.getId());
        request.setStatus(parseStatus(excelUtil.readString(row.getCell(4))));
        request.setEmployeeType(parseEmployeeType(excelUtil.readString(row.getCell(5))));
        request.setEntryDate(Objects.requireNonNullElse(excelUtil.readLocalDate(row.getCell(6)), LocalDate.now()));
        request.setEmail(blankToNull(excelUtil.readString(row.getCell(7))));
        request.setLeaderId(findLeaderIdByPhone(blankToNull(excelUtil.readString(row.getCell(8)))));
        request.setRoleIds(findRoleIdsByNames(blankToNull(excelUtil.readString(row.getCell(9)))));
        request.setRemark(blankToNull(excelUtil.readString(row.getCell(10))));
        return request;
    }

    private boolean isEmptyRow(Row row, int cellCount) {
        for (int i = 0; i < cellCount; i++) {
            if (StringUtils.hasText(excelUtil.readString(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private void ensurePhoneNotExists(String phone) {
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Employee::getPhone, phone));
        if (count != null && count > 0) {
            throw new BusinessException("手机号 " + phone + " 已存在");
        }
    }

    private Department getOrCreateDepartment(String departmentName) {
        Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Department::getDeptName, departmentName)
                .last("LIMIT 1"));
        if (department != null) {
            return department;
        }
        Department created = new Department();
        created.setTenantCode(TenantPermissionContext.getTenantCode());
        created.setDeptName(departmentName);
        created.setDeptCode(codeGeneratorUtil.generateCode("DPT", 4));
        created.setSortNo(99);
        created.setStatus(1);
        created.setIsDeleted(0);
        departmentMapper.insert(created);
        return created;
    }

    private Position getOrCreatePosition(String positionName, Long departmentId) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Position::getPositionName, positionName)
                .last("LIMIT 1"));
        if (position != null) {
            return position;
        }
        Position created = new Position();
        created.setTenantCode(TenantPermissionContext.getTenantCode());
        created.setPositionName(positionName);
        created.setPositionCode(codeGeneratorUtil.generateCode("POS", 4));
        created.setDepartmentId(departmentId);
        created.setSortNo(99);
        created.setStatus(1);
        created.setIsDeleted(0);
        positionMapper.insert(created);
        return created;
    }

    private Long findLeaderIdByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        Employee leader = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Employee::getPhone, phone)
                .last("LIMIT 1"));
        if (leader == null) {
            throw new BusinessException("直属领导手机号 " + phone + " 未找到");
        }
        return leader.getId();
    }

    private List<Long> findRoleIdsByNames(String roleNames) {
        if (!StringUtils.hasText(roleNames)) {
            return Collections.emptyList();
        }
        List<String> names = List.of(roleNames.split("[,，、;；]")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SysRole::getIsDeleted, 0)
                .in(SysRole::getRoleName, names));
        if (roles.size() != names.size()) {
            List<String> existed = roles.stream().map(SysRole::getRoleName).toList();
            String missing = names.stream().filter(name -> !existed.contains(name)).findFirst().orElse("未知角色");
            throw new BusinessException("角色 " + missing + " 不存在");
        }
        return roles.stream().map(SysRole::getId).toList();
    }

    private Integer parseStatus(String statusText) {
        if (!StringUtils.hasText(statusText) || "在职".equals(statusText)) {
            return STATUS_ACTIVE;
        }
        if ("试用".equals(statusText)) {
            return STATUS_PROBATION;
        }
        if ("离职".equals(statusText)) {
            return STATUS_RESIGNED;
        }
        throw new BusinessException("状态仅支持：在职、试用、离职");
    }

    private String parseEmployeeType(String employeeType) {
        if (!StringUtils.hasText(employeeType) || "全职".equals(employeeType)) {
            return "FULL_TIME";
        }
        if ("合同工".equals(employeeType)) {
            return "CONTRACT";
        }
        if ("试用期".equals(employeeType)) {
            return "PROBATION";
        }
        throw new BusinessException("员工类型仅支持：全职、合同工、试用期");
    }

    private String employeeTypeLabel(String employeeType) {
        if ("FULL_TIME".equals(employeeType)) {
            return "全职";
        }
        if ("CONTRACT".equals(employeeType)) {
            return "合同工";
        }
        if ("PROBATION".equals(employeeType)) {
            return "试用期";
        }
        return employeeType == null ? "" : employeeType;
    }

    private String statusLabelCn(Integer status) {
        if (Integer.valueOf(STATUS_ACTIVE).equals(status)) {
            return "在职";
        }
        if (Integer.valueOf(STATUS_PROBATION).equals(status)) {
            return "试用";
        }
        if (Integer.valueOf(STATUS_RESIGNED).equals(status)) {
            return "离职";
        }
        return "未知";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
