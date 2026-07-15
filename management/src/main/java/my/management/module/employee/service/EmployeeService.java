package my.management.module.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.common.utils.ExcelUtil;
import my.management.common.utils.PermissionCacheUtil;
import my.hive.shared.utils.EncryptUtil;
import my.management.common.vo.ImportResultVO;
import my.hive.domain.attendance.mapper.TenantAttendanceLocationManageMapper;
import my.hive.domain.attendance.model.entity.TenantAttendanceLocation;
import my.management.module.employee.mapper.DepartmentMapper;
import my.management.module.employee.mapper.EmployeeAttendanceLocationMapper;
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
import my.management.module.employee.model.entity.EmployeeAttendanceLocation;
import my.management.module.employee.model.entity.EmployeeChangeLog;
import my.management.module.employee.model.entity.EmployeeExt;
import my.management.module.employee.model.entity.Position;
import my.management.module.employee.model.enums.EmployeeStatusEnum;
import my.management.module.employee.model.enums.EmployeeTypeEnum;
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
import my.management.module.tenant.service.TenantFieldConfigService;
import my.management.module.tenant.service.TenantLicenseService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * EmployeeService 属于管理端后端员工模块，实现核心业务编排与规则逻辑。
 */
@Service
public class EmployeeService {

    private static final String DEFAULT_EMPLOYEE_TYPE = EmployeeTypeEnum.FULL_TIME.getCode();
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int EMPLOYEE_IMPORT_COLUMN_COUNT = 11;
    private static final int MAX_EMPLOYEE_IMPORT_ROWS = 5000;
    private static final int MAX_EMPLOYEE_EXPORT_ROWS = 10000;
    private static final long MAX_IMPORT_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_DEPARTMENT_LENGTH = 64;
    private static final int MAX_POSITION_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 128;
    private static final int MAX_LEADER_NAME_LENGTH = 64;
    private static final int MAX_ROLE_NAMES_LENGTH = 500;
    private static final int MAX_REMARK_LENGTH = 500;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final List<String> EMPLOYEE_IMPORT_HEADERS = List.of(
            "姓名", "手机号", "部门", "职位", "状态", "员工类型", "入职日期", "邮箱", "直属领导姓名", "角色名称", "备注");

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

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    @Resource
    private PrivacyProtectionUtil privacyProtectionUtil;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private TenantFieldConfigService tenantFieldConfigService;

    @Resource
    private TenantAttendanceLocationManageMapper tenantAttendanceLocationManageMapper;

    @Resource
    private EmployeeAttendanceLocationMapper employeeAttendanceLocationMapper;

    /**
     * 新员工初始密码从配置读取，避免敏感默认值固定写死在代码中。
     */
    @Value("${app.default-password.employee}")
    private String defaultPassword;

    public Page<EmployeePageVO> page(EmployeePageQuery query) {
        if (query == null) {
            query = new EmployeePageQuery();
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        Page<EmployeePageVO> page = new Page<>(safePageNum(query.getPage()), safePageSize(query.getSize()));
        Page<EmployeePageVO> result = employeeMapper.selectEmployeePage(
                page,
                tenantCode,
                query.getKeyword(),
                phoneKeywordHash(query.getKeyword()),
                query.getDepartmentId(),
                query.getStatus(),
                query.getEmployeeType(),
                query.getEntryDateStart(),
                query.getEntryDateEnd());
        fillViewFields(result.getRecords());
        return result;
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public EmployeeStatsVO stats() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        EmployeeStatsVO vo = new EmployeeStatsVO();
        Long totalEmployees = nvl(employeeMapper.countAvailableEmployees(tenantCode));
        Long attendanceUsers = nvl(employeeMapper.countTodayAttendanceUsers(tenantCode));
        vo.setTotalEmployees(totalEmployees);
        vo.setDepartmentCount(nvl(employeeMapper.countDistinctDepartments(tenantCode)));
        vo.setPendingOnboardCount(nvl(employeeMapper.countPendingOnboard(tenantCode)));
        vo.setTodayAttendanceRate(totalEmployees == 0 ? 0D : Math.round(attendanceUsers * 10000.0 / totalEmployees) / 100.0);
        return vo;
    }

    public EmployeeDetailVO detail(Long id) {
        EmployeeDetailVO detail = employeeMapper.selectEmployeeDetail(TenantPermissionContext.getTenantCode(), id);
        if (detail == null) {
            throw new BusinessException("employee not found");
        }
        fillViewFields(List.of(detail));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(@Valid EmployeeCreateRequest request) {
        tenantLicenseService.ensureUserQuotaAvailable(TenantPermissionContext.getTenantCode());
        Department department = requireDepartment(request.getDepartmentId());
        Position position = requirePosition(request.getPositionId());
        String leaderName = normalizeLeaderName(request.getLeaderName());

        String employeeType = normalizeEmployeeType(request.getEmployeeType(), DEFAULT_EMPLOYEE_TYPE);
        String normalizedPhone = requireNormalizedPhone(request.getPhone());
        String phoneHash = privacyProtectionUtil.hashPhone(normalizedPhone);
        Employee employee = findReusableJoinedEmployee(normalizedPhone, phoneHash);
        boolean reuseJoinedUser = employee != null;
        String phoneMask = privacyProtectionUtil.maskPhone(normalizedPhone);
        String empNo = codeGeneratorUtil.generateEmployeeNo();

        if (!reuseJoinedUser) {
            employee = new Employee();
            employee.setTenantCode(TenantPermissionContext.getTenantCode());
        }
        employee.setName(request.getName());
        // 登录账号使用工号，手机号只以不可逆哈希参与登录和查重，避免明文落库。
        employee.setLoginName(empNo);
        employee.setPhone(phoneMask);
        employee.setPhoneHash(phoneHash);
        employee.setPhoneMask(phoneMask);
        employee.setPassword(encryptUtil.encode(defaultPassword));
        employee.setMustChangePassword(1);
        employee.setDepartmentName(department.getDeptName());
        employee.setPosition(position.getPositionName());
        employee.setManagerId(null);
        employee.setManagerName(leaderName);
        employee.setStatus(request.getStatus());
        employee.setAttendanceRequired(normalizeAttendanceRequired(request.getAttendanceRequired()));
        if (reuseJoinedUser) {
            employeeMapper.updateById(employee);
            rotateAuthVersion(employee.getId());
        } else {
            employee.setPermissionVersion(1L);
            employee.setAuthVersion(1L);
            employeeMapper.insert(employee);
        }

        EmployeeExt ext = new EmployeeExt();
        ext.setUserId(employee.getId());
        ext.setTenantCode(TenantPermissionContext.getTenantCode());
        ext.setEmpNo(empNo);
        ext.setEmail(request.getEmail());
        ext.setEmployeeType(employeeType);
        ext.setEntryDate(request.getEntryDate());
        ext.setRemark(request.getRemark());
        ext.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        employeeExtMapper.insert(ext);
        syncUserRolesByStatus(employee.getId(), request.getRoleIds(), employee.getStatus());
        syncAttendanceLocations(employee.getId(), isResigned(employee.getStatus()) ? Collections.emptyList() : request.getAttendanceLocationIds());

        insertChangeLog(employee.getId(), reuseJoinedUser ? "COMPLETE_JOINED_USER_PROFILE" : "CREATE", null, Map.of(
                "name", employee.getName(),
                "loginName", employee.getLoginName(),
                "phone", phoneMask,
                "departmentName", employee.getDepartmentName(),
                "position", employee.getPosition(),
                "managerName", employee.getManagerName(),
                "status", employee.getStatus(),
                "attendanceRequired", employee.getAttendanceRequired(),
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
        String leaderName = normalizeLeaderName(request.getLeaderName());
        EmployeeExt ext = getOrCreateExt(employee.getId());
        if (!StringUtils.hasText(ext.getEmpNo())) {
            ext.setEmpNo(codeGeneratorUtil.generateEmployeeNo());
        }
        boolean keepExistingPhone = privacyProtectionUtil.isMasked(request.getPhone());
        String normalizedPhone = keepExistingPhone ? null : requireNormalizedPhone(request.getPhone());
        String phoneMask = keepExistingPhone ? privacyProtectionUtil.maskPhone(employee.getPhone()) : privacyProtectionUtil.maskPhone(normalizedPhone);
        if (!keepExistingPhone) {
            ensurePhoneNotExistsForUpdate(employee.getId(), normalizedPhone);
        }

        boolean syncLoginName = employee.getLoginName() == null || employee.getLoginName().isBlank() || employee.getLoginName().equals(employee.getPhone());
        employee.setName(request.getName());
        if (!keepExistingPhone) {
            employee.setPhone(phoneMask);
            employee.setPhoneHash(privacyProtectionUtil.hashPhone(normalizedPhone));
            employee.setPhoneMask(phoneMask);
        }
        if (syncLoginName) {
            employee.setLoginName(ext.getEmpNo());
        }
        employee.setDepartmentName(department.getDeptName());
        employee.setPosition(position.getPositionName());
        employee.setManagerId(null);
        employee.setManagerName(leaderName);
        employee.setStatus(request.getStatus());
        employee.setAttendanceRequired(normalizeAttendanceRequired(request.getAttendanceRequired()));
        employeeMapper.updateById(employee);
        rotateAuthVersion(employee.getId());

        ext.setEmail(request.getEmail());
        // Preserve the stored type on update when the caller omits the field.
        ext.setEmployeeType(normalizeEmployeeType(request.getEmployeeType(), ext.getEmployeeType()));
        ext.setEntryDate(request.getEntryDate());
        ext.setRemark(request.getRemark());
        saveOrUpdateExt(ext);
        syncUserRolesByStatus(employee.getId(), request.getRoleIds(), employee.getStatus());
        syncAttendanceLocations(employee.getId(), isResigned(employee.getStatus()) ? Collections.emptyList() : request.getAttendanceLocationIds());

        insertChangeLog(employee.getId(), "UPDATE", before, detail(employee.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(@Valid EmployeeStatusChangeRequest request) {
        Employee employee = requireEmployee(request.getId());
        Integer beforeStatus = employee.getStatus();
        employee.setStatus(request.getStatus());
        employeeMapper.updateById(employee);
        if (!Objects.equals(beforeStatus, request.getStatus())) {
            rotateAuthVersion(employee.getId());
        }
        if (isResigned(request.getStatus())) {
            revokeUserRoles(employee.getId());
        }
        if (StringUtils.hasText(request.getRemark())) {
            EmployeeExt ext = getOrCreateExt(employee.getId());
            ext.setRemark(request.getRemark());
            saveOrUpdateExt(ext);
        }
        insertChangeLog(employee.getId(), "CHANGE_STATUS", Map.of("status", beforeStatus), Map.of("status", request.getStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(@Valid EmployeeBatchUpdateRequest request) {
        if (request.getDepartmentId() == null && request.getPositionId() == null && request.getLeaderName() == null
                && request.getStatus() == null && !StringUtils.hasText(request.getRemark())) {
            throw new BusinessException("at least one field is required for batch update");
        }

        Department department = request.getDepartmentId() == null ? null : requireDepartment(request.getDepartmentId());
        Position position = request.getPositionId() == null ? null : requirePosition(request.getPositionId());
        String leaderName = request.getLeaderName() == null ? null : normalizeLeaderName(request.getLeaderName());

        for (Long id : request.getIds()) {
            Employee employee = requireEmployee(id);
            EmployeeDetailVO before = detail(id);
            Integer beforeStatus = employee.getStatus();
            if (department != null) {
                employee.setDepartmentName(department.getDeptName());
            }
            if (position != null) {
                employee.setPosition(position.getPositionName());
            }
            if (request.getLeaderName() != null) {
                employee.setManagerId(null);
                employee.setManagerName(leaderName);
            }
            if (request.getStatus() != null) {
                employee.setStatus(request.getStatus());
            }
            employeeMapper.updateById(employee);
            if (request.getStatus() != null && !Objects.equals(beforeStatus, request.getStatus())) {
                rotateAuthVersion(employee.getId());
            }
            if (isResigned(request.getStatus())) {
                revokeUserRoles(employee.getId());
            }

            if (StringUtils.hasText(request.getRemark())) {
                EmployeeExt ext = getOrCreateExt(id);
                ext.setRemark(request.getRemark());
                saveOrUpdateExt(ext);
            }
            insertChangeLog(id, "BATCH_UPDATE", before, detail(id));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markResignedByApproval(Long employeeId, String remark) {
        Employee employee = requireEmployee(employeeId);
        if (isResigned(employee.getStatus())) {
            revokeUserRoles(employee.getId());
            return;
        }
        Integer beforeStatus = employee.getStatus();
        employee.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
        employeeMapper.updateById(employee);
        rotateAuthVersion(employee.getId());
        revokeUserRoles(employee.getId());
        if (StringUtils.hasText(remark)) {
            EmployeeExt ext = getOrCreateExt(employeeId);
            String oldRemark = ext.getRemark();
            String auditRemark = "离职审批通过：" + remark.trim();
            ext.setRemark(StringUtils.hasText(oldRemark) ? oldRemark + "\n" + auditRemark : auditRemark);
            saveOrUpdateExt(ext);
        }
        insertChangeLog(employeeId, "RESIGNATION_APPROVED",
                Map.of("status", beforeStatus),
                Map.of("status", EmployeeStatusEnum.RESIGNED.getCode()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Employee employee = requireEmployee(id);
        if (!isResigned(employee.getStatus())) {
            employee.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
            employeeMapper.updateById(employee);
            rotateAuthVersion(employee.getId());
        }
        revokeUserRoles(employee.getId());
        EmployeeExt ext = getOrCreateExt(id);
        ext.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
        saveOrUpdateExt(ext);
        insertChangeLog(id, "DELETE",
                Map.of("isDeleted", DeleteFlagEnum.NORMAL.getCode()),
                Map.of("isDeleted", DeleteFlagEnum.DELETED.getCode()));
    }

    public List<EmployeeLeaderOptionVO> searchLeaders(String keyword, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return employeeMapper.searchLeaders(TenantPermissionContext.getTenantCode(), keyword, safeLimit);
    }

    public EmployeeFormOptionsVO initFormOptions() {
        EmployeeFormOptionsVO vo = new EmployeeFormOptionsVO();
        vo.setDepartments(departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                        .eq(Department::getStatus, CommonStatusEnum.ENABLED.getCode())
                        .orderByAsc(Department::getSortNo))
                .stream()
                .map(this::toDepartmentOption)
                .toList());
        vo.setPositions(positionMapper.selectList(new LambdaQueryWrapper<Position>()
                        .eq(Position::getStatus, CommonStatusEnum.ENABLED.getCode())
                        .orderByAsc(Position::getSortNo))
                .stream()
                .map(this::toPositionOption)
                .toList());
        vo.setRoles(sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                        .orderByDesc(SysRole::getIsSystem)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(this::toRoleOption)
                .toList());
        vo.setAttendanceLocations(tenantAttendanceLocationManageMapper.selectActiveByTenantCode(TenantPermissionContext.getTenantCode())
                .stream()
                .map(location -> new OptionVO(location.getLocationName(), String.valueOf(location.getId())))
                .toList());
        vo.setEmployeeTypes(List.of(
                new OptionVO("Full Time", EmployeeTypeEnum.FULL_TIME.getCode()),
                new OptionVO("Contract", EmployeeTypeEnum.CONTRACT.getCode()),
                new OptionVO("Probation", EmployeeTypeEnum.PROBATION.getCode())
        ));
        vo.setEmploymentStatuses(List.of(
                new OptionVO("Resigned", String.valueOf(EmployeeStatusEnum.RESIGNED.getCode())),
                new OptionVO("Active", String.valueOf(EmployeeStatusEnum.ACTIVE.getCode())),
                new OptionVO("Probation", String.valueOf(EmployeeStatusEnum.PROBATION.getCode()))
        ));
        return vo;
    }

    public List<EmployeePageVO> export(EmployeePageQuery query) {
        if (query == null) {
            query = new EmployeePageQuery();
        }
        List<EmployeePageVO> list = employeeMapper.selectEmployeeExport(
                TenantPermissionContext.getTenantCode(),
                query.getKeyword(),
                phoneKeywordHash(query.getKeyword()),
                query.getDepartmentId(),
                query.getStatus(),
                query.getEmployeeType(),
                query.getEntryDateStart(),
                query.getEntryDateEnd(),
                MAX_EMPLOYEE_EXPORT_ROWS + 1);
        if (list.size() > MAX_EMPLOYEE_EXPORT_ROWS) {
            throw new BusinessException("导出数据超过 " + MAX_EMPLOYEE_EXPORT_ROWS + " 行，请缩小筛选范围后重试");
        }
        fillViewFields(list);
        return list;
    }

    public void exportExcel(EmployeePageQuery query, HttpServletResponse response) {
        Map<String, String> fieldLabels = currentTenantFieldLabels("employee");
        List<String> headers = employeeExportHeaders(fieldLabels);
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
        excelUtil.writeRowsToResponse(response,
                "员工列表",
                headers,
                rows,
                "员工列表.xlsx");
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        List<String> headers = employeeImportHeaders(currentTenantFieldLabels("employee"));
        List<List<String>> examples = List.of(
                List.of("张三", "13900030001", "仓储部", "仓库专员", "在职", "全职", LocalDate.now().toString(), "zhangsan@example.com", "王主管", "普通员工", "示例数据"),
                List.of("李四", "13900030002", "业务部", "销售专员", "试用", "试用期", LocalDate.now().toString(), "lisi@example.com", "刘经理", "销售专员,普通员工", "")
        );
        List<String> notes = List.of(
                "仅支持 .xlsx 文件导入。",
                "必填列：姓名、手机号、部门、职位。",
                "状态支持：在职、试用、离职。为空时默认在职。",
                "员工类型支持：全职、合同工、试用期。为空时默认全职。",
                "入职日期格式：yyyy-MM-dd。为空时默认当天。",
                "直属领导姓名可不填；填写后将直接保存到员工主数据。",
                "角色名称可填多个，使用英文逗号、中文逗号或顿号分隔；角色必须已存在。",
                "若部门或职位不存在，系统会自动创建启用中的部门和职位。"
        );
        excelUtil.writeTemplateToResponse(response,
                "员工导入模板",
                headers,
                examples,
                notes,
                "员工导入模板.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importEmployees(MultipartFile file) {
        excelUtil.validateXlsxImportFile(file, MAX_IMPORT_FILE_SIZE_BYTES);
        ImportResultVO result = new ImportResultVO();
        Set<String> importedPhones = new HashSet<>();
        try (var inputStream = file.getInputStream(); var workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            excelUtil.validateImportHeaderOptions(sheet.getRow(0), List.of(EMPLOYEE_IMPORT_HEADERS, employeeImportHeaders(currentTenantFieldLabels("employee"))));
            excelUtil.validateImportDataRows(sheet, EMPLOYEE_IMPORT_COLUMN_COUNT, MAX_EMPLOYEE_IMPORT_ROWS);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (excelUtil.isEmptyRow(row, EMPLOYEE_IMPORT_COLUMN_COUNT)) {
                    continue;
                }
                result.setTotalCount(result.getTotalCount() + 1);
                try {
                    EmployeeCreateRequest request = buildImportRequest(row);
                    String normalizedPhone = requireNormalizedPhone(request.getPhone());
                    if (!importedPhones.add(normalizedPhone)) {
                        throw new BusinessException("手机号在导入文件中重复：" + privacyProtectionUtil.maskPhone(normalizedPhone));
                    }
                    create(request);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    if (result.getFailMessages().size() < 20) {
                        result.getFailMessages().add("第 " + (i + 1) + " 行：" + ex.getMessage());
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("读取员工导入文件失败");
        } catch (Exception e) {
            throw new BusinessException("员工导入文件格式不正确，请使用系统下载的 .xlsx 模板");
        }
        return result;
    }

    private Employee requireEmployee(Long id) {
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Employee::getId, id)
                .last("LIMIT 1"));
        if (employee == null) {
            throw new BusinessException("employee not found");
        }
        EmployeeExt ext = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>()
                .eq(EmployeeExt::getUserId, id)
                .last("LIMIT 1"));
        if (ext != null && DeleteFlagEnum.isDeleted(ext.getIsDeleted())) {
            throw new BusinessException("employee has been deleted");
        }
        return employee;
    }

    private Department requireDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null || DeleteFlagEnum.isDeleted(department.getIsDeleted()) || !CommonStatusEnum.isEnabled(department.getStatus())) {
            throw new BusinessException("department is invalid");
        }
        return department;
    }

    private Position requirePosition(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null || DeleteFlagEnum.isDeleted(position.getIsDeleted()) || !CommonStatusEnum.isEnabled(position.getStatus())) {
            throw new BusinessException("position is invalid");
        }
        return position;
    }

    private String normalizeLeaderName(String leaderName) {
        if (!StringUtils.hasText(leaderName)) {
            return null;
        }
        String normalized = leaderName.trim();
        if (normalized.length() > 64) {
            throw new BusinessException("leader name is too long");
        }
        return normalized;
    }

    private String normalizeEmployeeType(String employeeType, String fallback) {
        if (StringUtils.hasText(employeeType)) {
            return employeeType.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return DEFAULT_EMPLOYEE_TYPE;
    }

    private EmployeeExt getOrCreateExt(Long userId) {
        EmployeeExt ext = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>()
                .eq(EmployeeExt::getUserId, userId)
                .last("LIMIT 1"));
        if (ext != null) {
            return ext;
        }
        EmployeeExt created = new EmployeeExt();
        created.setUserId(userId);
        created.setTenantCode(TenantPermissionContext.getTenantCode());
        created.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        return created;
    }

    private void saveOrUpdateExt(EmployeeExt ext) {
        if (ext.getId() == null) {
            employeeExtMapper.insert(ext);
        } else {
            employeeExtMapper.updateById(ext);
        }
    }

    private void fillViewFields(List<? extends EmployeePageVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<Long> userIds = list.stream()
                .map(EmployeePageVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<Long>> roleIdMap = new HashMap<>();
        Map<Long, List<String>> roleNameMap = new HashMap<>();
        Map<Long, List<Long>> attendanceLocationIdMap = new HashMap<>();
        Map<Long, List<String>> attendanceLocationNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserRoleMapper.selectRoleIdsByUserIds(tenantCode, userIds).forEach(row -> {
                Long userId = toLong(row.get("userId"));
                Long roleId = toLong(row.get("roleId"));
                if (userId != null && roleId != null) {
                    roleIdMap.computeIfAbsent(userId, key -> new ArrayList<>()).add(roleId);
                }
            });
            sysUserRoleMapper.selectRoleNamesByUserIds(tenantCode, userIds).forEach(row -> {
                Long userId = toLong(row.get("userId"));
                String roleName = row.get("roleName") == null ? null : String.valueOf(row.get("roleName"));
                if (userId != null && StringUtils.hasText(roleName)) {
                    roleNameMap.computeIfAbsent(userId, key -> new ArrayList<>()).add(roleName);
                }
            });
            List<EmployeeAttendanceLocation> relations = employeeAttendanceLocationMapper.selectList(new LambdaQueryWrapper<EmployeeAttendanceLocation>()
                    .in(EmployeeAttendanceLocation::getUserId, userIds));
            Map<Long, String> locationNameById = new HashMap<>();
            List<Long> locationIds = (relations == null ? List.<EmployeeAttendanceLocation>of() : relations).stream()
                    .map(EmployeeAttendanceLocation::getAttendanceLocationId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!locationIds.isEmpty()) {
                tenantAttendanceLocationManageMapper.selectList(new LambdaQueryWrapper<TenantAttendanceLocation>()
                        .eq(TenantAttendanceLocation::getStatus, CommonStatusEnum.ENABLED.getCode())
                        .in(TenantAttendanceLocation::getId, locationIds)
                        .orderByAsc(TenantAttendanceLocation::getSortOrder)
                        .orderByAsc(TenantAttendanceLocation::getId))
                        .forEach(location -> locationNameById.put(location.getId(), location.getLocationName()));
            }
            for (EmployeeAttendanceLocation relation : (relations == null ? List.<EmployeeAttendanceLocation>of() : relations)) {
                if (relation.getUserId() == null || relation.getAttendanceLocationId() == null) {
                    continue;
                }
                attendanceLocationIdMap.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(relation.getAttendanceLocationId());
                String locationName = locationNameById.get(relation.getAttendanceLocationId());
                if (StringUtils.hasText(locationName)) {
                    attendanceLocationNameMap.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(locationName);
                }
            }
        }

        list.forEach(vo -> {
            vo.setStatusLabel(statusLabel(vo.getStatus()));
            vo.setPhone(privacyProtectionUtil.maskPhone(vo.getPhone()));
            vo.setAttendanceRequired(normalizeAttendanceRequired(vo.getAttendanceRequired()));
            vo.setRoleIds(roleIdMap.getOrDefault(vo.getId(), Collections.emptyList()));
            vo.setRoleNames(roleNameMap.getOrDefault(vo.getId(), Collections.emptyList()));
            vo.setAttendanceLocationIds(attendanceLocationIdMap.getOrDefault(vo.getId(), Collections.emptyList()));
            vo.setAttendanceLocationNames(attendanceLocationNameMap.getOrDefault(vo.getId(), Collections.emptyList()));
        });
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
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String statusLabel(Integer status) {
        return EmployeeStatusEnum.of(status).getLabel();
    }

    private Integer normalizeAttendanceRequired(Integer value) {
        return value != null && value == 0 ? 0 : 1;
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
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        validateAssignableRoles(tenantCode, normalizedRoleIds);

        List<SysUserRole> existed = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantCode, tenantCode)
                .eq(SysUserRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        for (SysUserRole item : existed) {
            item.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
            sysUserRoleMapper.updateById(item);
        }

        if (!normalizedRoleIds.isEmpty()) {
            for (Long roleId : normalizedRoleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setTenantCode(tenantCode);
                userRole.setRoleId(roleId);
                userRole.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
                sysUserRoleMapper.insert(userRole);
            }
        }
        rotatePermissionVersion(userId);
    }

    private void syncUserRolesByStatus(Long userId, List<Long> roleIds, Integer status) {
        if (isResigned(status)) {
            revokeUserRoles(userId);
            syncAttendanceLocations(userId, Collections.emptyList());
            return;
        }
        syncUserRoles(userId, roleIds);
    }

    private void syncAttendanceLocations(Long userId, List<Long> locationIds) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<Long> normalizedLocationIds = normalizeAttendanceLocationIds(tenantCode, locationIds);
        employeeAttendanceLocationMapper.delete(new LambdaQueryWrapper<EmployeeAttendanceLocation>()
                .eq(EmployeeAttendanceLocation::getUserId, userId));
        for (Long locationId : normalizedLocationIds) {
            EmployeeAttendanceLocation relation = new EmployeeAttendanceLocation();
            relation.setTenantCode(tenantCode);
            relation.setUserId(userId);
            relation.setAttendanceLocationId(locationId);
            employeeAttendanceLocationMapper.insert(relation);
        }
    }

    private void revokeUserRoles(Long userId) {
        if (userId == null) {
            return;
        }
        List<SysUserRole> existed = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SysUserRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        for (SysUserRole item : existed) {
            item.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
            sysUserRoleMapper.updateById(item);
        }
        if (!existed.isEmpty()) {
            rotatePermissionVersion(userId);
        }
    }

    private void rotatePermissionVersion(Long userId) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (employeeMapper.incrementPermissionVersion(tenantCode, userId) != 1) {
            throw new BusinessException(404, "员工不存在");
        }
        permissionCacheUtil.evict(tenantCode, userId);
    }

    private void rotateAuthVersion(Long userId) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (employeeMapper.incrementAuthVersion(tenantCode, userId) != 1) {
            throw new BusinessException(404, "员工不存在");
        }
        permissionCacheUtil.evict(tenantCode, userId);
    }

    private boolean isResigned(Integer status) {
        return EmployeeStatusEnum.RESIGNED.getCode().equals(status);
    }

    /**
     * 角色 ID 由前端多选框传入，历史数据中可能存在重复关联，保存前统一去重并过滤空值。
     */
    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .filter(roleId -> roleId > 0)
                .distinct()
                .toList();
    }

    private List<Long> normalizeAttendanceLocationIds(String tenantCode, List<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> normalized = locationIds.stream()
                .filter(Objects::nonNull)
                .filter(locationId -> locationId > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        List<TenantAttendanceLocation> locations = tenantAttendanceLocationManageMapper.selectList(new LambdaQueryWrapper<TenantAttendanceLocation>()
                .eq(TenantAttendanceLocation::getStatus, CommonStatusEnum.ENABLED.getCode())
                .in(TenantAttendanceLocation::getId, normalized));
        Set<Long> validLocationIds = new HashSet<>(locations.stream().map(TenantAttendanceLocation::getId).toList());
        if (validLocationIds.size() != normalized.size()) {
            throw new BusinessException("存在无效打卡地点，无法分配");
        }
        return normalized;
    }

    /**
     * 先校验角色再清空旧关联，避免无效角色导致事务回滚时给排查带来干扰。
     */
    private void validateAssignableRoles(String tenantCode, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .in(SysRole::getId, roleIds));
        Set<Long> validRoleIds = new HashSet<>(roles.stream().map(SysRole::getId).toList());
        if (validRoleIds.size() != roleIds.size()) {
            throw new BusinessException("存在无效角色，无法分配");
        }
    }

    private EmployeeCreateRequest buildImportRequest(Row row) {
        String name = requireImportText("姓名", excelUtil.readString(row.getCell(0)), MAX_NAME_LENGTH);
        String phone = requireNormalizedImportPhone(excelUtil.readString(row.getCell(1)));
        String departmentName = requireImportText("部门", excelUtil.readString(row.getCell(2)), MAX_DEPARTMENT_LENGTH);
        String positionName = requireImportText("职位", excelUtil.readString(row.getCell(3)), MAX_POSITION_LENGTH);
        String statusText = optionalImportText("状态", excelUtil.readString(row.getCell(4)), 10);
        String employeeTypeText = optionalImportText("员工类型", excelUtil.readString(row.getCell(5)), 20);
        LocalDate entryDate = readImportDate(row, 6, "入职日期", LocalDate.now());
        String email = optionalImportText("邮箱", excelUtil.readString(row.getCell(7)), MAX_EMAIL_LENGTH);
        validateImportEmail(email);
        String leaderName = optionalImportText("直属领导姓名", excelUtil.readString(row.getCell(8)), MAX_LEADER_NAME_LENGTH);
        String roleNames = optionalImportText("角色名称", excelUtil.readString(row.getCell(9)), MAX_ROLE_NAMES_LENGTH);
        String remark = optionalImportText("备注", excelUtil.readString(row.getCell(10)), MAX_REMARK_LENGTH);

        Department department = getOrCreateDepartment(departmentName);
        Position position = getOrCreatePosition(positionName, department.getId());

        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setDepartmentId(department.getId());
        request.setPositionId(position.getId());
        request.setStatus(parseStatus(statusText));
        request.setEmployeeType(parseEmployeeType(employeeTypeText));
        request.setEntryDate(entryDate);
        request.setEmail(email);
        request.setLeaderName(leaderName);
        request.setRoleIds(findRoleIdsByNames(roleNames));
        request.setRemark(remark);
        return request;
    }

    /**
     * 小程序一键登录会先产生一条没有 emp_employee_ext 档案的 user。
     * 管理端新增/导入同手机号员工时应补全这条记录，而不是再新建一条或直接报重复。
     */
    private Employee findReusableJoinedEmployee(String normalizedPhone, String phoneHash) {
        List<Employee> matchedUsers = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .and(wrapper -> wrapper.eq(Employee::getPhoneHash, phoneHash).or().eq(Employee::getPhone, normalizedPhone))
                .last("LIMIT 2"));
        if (matchedUsers == null || matchedUsers.isEmpty()) {
            return null;
        }
        if (matchedUsers.size() > 1) {
            throw new BusinessException("手机号 " + privacyProtectionUtil.maskPhone(normalizedPhone) + " 存在多条账号，请先合并用户后再导入");
        }

        Employee matchedUser = matchedUsers.get(0);
        EmployeeExt activeExt = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>()
                .eq(EmployeeExt::getUserId, matchedUser.getId())
                .eq(EmployeeExt::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (activeExt != null) {
            throw new BusinessException("手机号 " + privacyProtectionUtil.maskPhone(normalizedPhone) + " 已存在");
        }
        return matchedUser;
    }

    private void ensurePhoneNotExistsForUpdate(Long employeeId, String phone) {
        String normalizedPhone = requireNormalizedPhone(phone);
        String phoneHash = privacyProtectionUtil.hashPhone(normalizedPhone);
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .ne(Employee::getId, employeeId)
                .and(wrapper -> wrapper.eq(Employee::getPhoneHash, phoneHash).or().eq(Employee::getPhone, normalizedPhone)));
        if (count != null && count > 0) {
            throw new BusinessException("手机号 " + privacyProtectionUtil.maskPhone(normalizedPhone) + " 已存在");
        }
    }

    private Department getOrCreateDepartment(String departmentName) {
        Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptName, departmentName)
                .eq(Department::getStatus, CommonStatusEnum.ENABLED.getCode())
                .eq(Department::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (department != null) {
            return department;
        }
        Department created = new Department();
        created.setTenantCode(TenantPermissionContext.getTenantCode());
        created.setDeptName(departmentName);
        created.setDeptCode(codeGeneratorUtil.generateCode("DPT", 4));
        created.setSortNo(99);
        created.setStatus(CommonStatusEnum.ENABLED.getCode());
        created.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        departmentMapper.insert(created);
        return created;
    }

    private Position getOrCreatePosition(String positionName, Long departmentId) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionName, positionName)
                .eq(Position::getDepartmentId, departmentId)
                .eq(Position::getStatus, CommonStatusEnum.ENABLED.getCode())
                .eq(Position::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
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
        created.setStatus(CommonStatusEnum.ENABLED.getCode());
        created.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        positionMapper.insert(created);
        return created;
    }

    private Long findLeaderIdByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String normalizedPhone = requireNormalizedPhone(phone);
        String phoneHash = privacyProtectionUtil.hashPhone(normalizedPhone);
        Employee leader = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .and(wrapper -> wrapper.eq(Employee::getPhoneHash, phoneHash).or().eq(Employee::getPhone, normalizedPhone))
                .last("LIMIT 1"));
        if (leader == null) {
            throw new BusinessException("直属领导手机号 " + privacyProtectionUtil.maskPhone(normalizedPhone) + " 未找到");
        }
        return leader.getId();
    }

    private String requireNormalizedPhone(String phone) {
        if (privacyProtectionUtil.isMasked(phone)) {
            throw new BusinessException("请填写完整手机号后再保存");
        }
        String normalizedPhone = privacyProtectionUtil.normalizePhone(phone);
        if (!StringUtils.hasText(normalizedPhone) || normalizedPhone.length() < 7 || normalizedPhone.length() > MAX_PHONE_LENGTH) {
            throw new BusinessException("手机号格式不正确");
        }
        return normalizedPhone;
    }

    private String phoneKeywordHash(String keyword) {
        if (!privacyProtectionUtil.mayBePhoneKeyword(keyword)) {
            return null;
        }
        return privacyProtectionUtil.hashPhone(keyword);
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
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .in(SysRole::getRoleName, names));
        if (roles.size() != names.size()) {
            List<String> existed = roles.stream().map(SysRole::getRoleName).toList();
            String missing = names.stream().filter(name -> !existed.contains(name)).findFirst().orElse("未知角色");
            throw new BusinessException("角色 " + missing + " 不存在");
        }
        return roles.stream().map(SysRole::getId).toList();
    }

    private Integer parseStatus(String statusText) {
        return EmployeeStatusEnum.parseCn(statusText).getCode();
    }

    private String parseEmployeeType(String employeeType) {
        return EmployeeTypeEnum.parseCn(employeeType).getCode();
    }

    private String employeeTypeLabel(String employeeType) {
        return EmployeeTypeEnum.of(employeeType).getLabel();
    }

    private String statusLabelCn(Integer status) {
        return EmployeeStatusEnum.of(status).getCnLabel();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> employeeExportHeaders(Map<String, String> fieldLabels) {
        return List.of(
                fieldLabel(fieldLabels, "name", "姓名"),
                fieldLabel(fieldLabels, "empNo", "工号"),
                fieldLabel(fieldLabels, "departmentName", "部门"),
                fieldLabel(fieldLabels, "positionName", "职位"),
                fieldLabel(fieldLabels, "email", "邮箱"),
                fieldLabel(fieldLabels, "phone", "电话"),
                fieldLabel(fieldLabels, "status", "状态"),
                fieldLabel(fieldLabels, "employeeType", "员工类型"),
                fieldLabel(fieldLabels, "leaderName", "直属领导"),
                "角色",
                fieldLabel(fieldLabels, "entryDate", "入职日期")
        );
    }

    private List<String> employeeImportHeaders(Map<String, String> fieldLabels) {
        return List.of(
                fieldLabel(fieldLabels, "name", EMPLOYEE_IMPORT_HEADERS.get(0)),
                fieldLabel(fieldLabels, "phone", EMPLOYEE_IMPORT_HEADERS.get(1)),
                fieldLabel(fieldLabels, "departmentName", EMPLOYEE_IMPORT_HEADERS.get(2)),
                fieldLabel(fieldLabels, "positionName", EMPLOYEE_IMPORT_HEADERS.get(3)),
                fieldLabel(fieldLabels, "status", EMPLOYEE_IMPORT_HEADERS.get(4)),
                fieldLabel(fieldLabels, "employeeType", EMPLOYEE_IMPORT_HEADERS.get(5)),
                fieldLabel(fieldLabels, "entryDate", EMPLOYEE_IMPORT_HEADERS.get(6)),
                fieldLabel(fieldLabels, "email", EMPLOYEE_IMPORT_HEADERS.get(7)),
                fieldLabel(fieldLabels, "leaderName", EMPLOYEE_IMPORT_HEADERS.get(8)),
                EMPLOYEE_IMPORT_HEADERS.get(9),
                fieldLabel(fieldLabels, "remark", EMPLOYEE_IMPORT_HEADERS.get(10))
        );
    }

    private String fieldLabel(Map<String, String> fieldLabels, String fieldKey, String fallback) {
        if (fieldLabels == null || !StringUtils.hasText(fieldKey)) {
            return fallback;
        }
        String label = fieldLabels.get(fieldKey);
        return StringUtils.hasText(label) ? label.trim() : fallback;
    }

    private Map<String, String> currentTenantFieldLabels(String moduleCode) {
        try {
            return tenantFieldConfigService.currentFieldLabelMap(moduleCode);
        } catch (BusinessException ignored) {
            return Collections.emptyMap();
        }
    }

    private String requireImportText(String fieldName, String value, int maxLength) {
        String normalized = optionalImportText(fieldName, value, maxLength);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String optionalImportText(String fieldName, String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String requireNormalizedImportPhone(String phone) {
        String normalizedPhone = requireNormalizedPhone(phone);
        if (normalizedPhone.length() > MAX_PHONE_LENGTH) {
            throw new BusinessException("手机号不能超过 " + MAX_PHONE_LENGTH + " 位");
        }
        return normalizedPhone;
    }

    private void validateImportEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    private LocalDate readImportDate(Row row, int cellIndex, String fieldName, LocalDate fallback) {
        try {
            LocalDate value = excelUtil.readLocalDate(row.getCell(cellIndex));
            return value == null ? fallback : value;
        } catch (DateTimeParseException ex) {
            throw new BusinessException(fieldName + "格式必须为 yyyy-MM-dd");
        }
    }
}
