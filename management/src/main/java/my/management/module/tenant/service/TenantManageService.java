package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.service.DeveloperAccessService;
import my.hive.common.utils.EncryptUtil;
import my.management.module.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import my.management.module.employee.mapper.DepartmentMapper;
import my.management.module.employee.mapper.EmployeeExtMapper;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.mapper.PositionMapper;
import my.management.module.employee.model.entity.Department;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.entity.EmployeeExt;
import my.management.module.employee.model.entity.Position;
import my.management.module.label.service.LabelTemplateService;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.mapper.TenantUsageMeterMapper;
import my.management.module.tenant.model.dto.TenantCreateRequest;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.dto.TenantPageRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.entity.TenantUsageMeter;
import my.management.module.tenant.model.enums.TenantUsageMeterEnum;
import my.management.module.tenant.model.vo.TenantDetailVO;
import my.management.module.tenant.model.vo.TenantPageVO;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * TenantManageService 属于管理端后端租户模块，实现核心业务编排与规则逻辑。
 */
@Service
public class TenantManageService {

    private static final String TENANT_OWNER_ROLE_CODE = "TENANT_OWNER";
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = buildReleaseLockScript();

    private static final Map<String, String> MODULE_ROLE_NAME_MAP = Map.of(
            "document:*", "文档管理员",
            "sales:order:*", "销售订单管理员",
            "production:order:*", "生产订单管理员",
            "inventory:*", "库存管理员",
            "badproduct:*", "次品管理员",
            "customer:*", "客户管理员",
            "attendance:*", "考勤管理员",
            "approval:*", "审批管理员",
            "dashboard:ai:*", "经营洞察管理员"
    );

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantUsageMeterMapper tenantUsageMeterMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private EmployeeExtMapper employeeExtMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private TenantAttendanceRuleManageMapper tenantAttendanceRuleManageMapper;

    @Resource
    private LabelTemplateService labelTemplateService;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private DeveloperAccessService developerAccessService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private TiandituGeocodeService tiandituGeocodeService;

    @Resource
    private TenantLicenseService tenantLicenseService;

    /**
     * 新租户负责人初始密码从配置读取，线上可通过环境变量单独轮换。
     */
    @Value("${app.default-password.tenant-owner}")
    private String defaultTenantPassword;

    public Page<TenantPageVO> page(TenantPageRequest request) {
        ensureDeveloperAccess();
        Page<Tenant> page = new Page<>(safePageNum(request.getCurrent()), safePageSize(request.getSize()));
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getDeleted, 0)
                .orderByDesc(Tenant::getCreateTime);

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(q -> q.like(Tenant::getTenantName, request.getKeyword().trim())
                    .or()
                    .like(Tenant::getTenantCode, request.getKeyword().trim())
                    .or()
                    .like(Tenant::getContactPerson, request.getKeyword().trim()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, request.getStatus());
        }

        Page<Tenant> tenantPage = tenantMapper.selectPage(page, wrapper);
        Page<TenantPageVO> result = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        List<TenantPageVO> records = tenantPage.getRecords().stream().map(this::toPageVO).toList();
        fillCurrentMonthUsage(records);
        result.setRecords(records);
        return result;
    }

    private long safePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public TenantDetailVO detail(Long id) {
        ensureDeveloperAccess();
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || DeleteFlagEnum.isDeleted(tenant.getDeleted())) {
            throw new BusinessException("租户不存在");
        }
        TenantDetailVO vo = new TenantDetailVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createTenant(TenantCreateRequest request) {
        ensureDeveloperAccess();
        String tenantCode = request.getTenantCode().trim().toUpperCase(Locale.ROOT);
        if (!tenantCode.matches("^[A-Z0-9_]+$")) {
            throw new BusinessException("租户编码仅支持大写字母、数字和下划线");
        }
        String lockKey = redisKeyBuilder.lock("tenant", "create", tenantCode);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("租户正在创建中，请稍后再试");
        }
        try {
            return doCreateTenant(request, tenantCode);
        } finally {
            releaseTenantCreateLock(lockKey, lockValue);
        }
    }

    private Long doCreateTenant(TenantCreateRequest request, String tenantCode) {
        Long count = tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, tenantCode));
        if (count != null && count > 0) {
            throw new BusinessException("租户编码已存在");
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(request.getTenantName().trim());
        tenant.setTenantType(request.getTenantType() == null ? 1 : request.getTenantType());
        tenant.setContactPerson(request.getContactPerson());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setPassword(encryptUtil.encode(request.getPassword() == null || request.getPassword().isBlank()
                ? defaultTenantPassword
                : request.getPassword().trim()));
        tenant.setStatus(CommonStatusEnum.ENABLED.getCode());
        tenant.setDeleted(DeleteFlagEnum.NORMAL.getCode());
        tenant.setCreator(TenantPermissionContext.getUserId());
        tenantLicenseService.applyDefaultTrial(tenant);
        tenantMapper.insert(tenant);

        Long ownerRoleId = initTenantOwnerRole(tenantCode);
        initTenantModuleRoles(tenantCode);
        Long ownerUserId = initTenantOwnerUser(tenantCode, request, ownerRoleId);
        initTenantOrgDefaults(tenantCode);
        initTenantAttendanceRule(tenantCode, request);
        labelTemplateService.ensureDefaultsForTenant(tenantCode, ownerUserId);
        clearTenantRuntimeCache(tenantCode);
        return tenant.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLicense(TenantLicenseUpdateRequest request) {
        ensureDeveloperAccess();
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException("租户授权参数不完整");
        }
        Tenant tenant = tenantMapper.selectById(request.getId());
        if (tenant == null || DeleteFlagEnum.isDeleted(tenant.getDeleted())) {
            throw new BusinessException("租户不存在");
        }
        tenantLicenseService.applyLicenseUpdate(tenant, request);
        tenantMapper.updateById(tenant);
        clearTenantRuntimeCache(tenant.getTenantCode());
    }

    private void clearTenantRuntimeCache(String tenantCode) {
        tenantLicenseService.clearTenantRuntimeCache(tenantCode);
    }

    private void releaseTenantCreateLock(String lockKey, String lockValue) {
        stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockValue);
    }

    private TenantPageVO toPageVO(Tenant tenant) {
        TenantPageVO vo = new TenantPageVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }

    private void fillCurrentMonthUsage(List<TenantPageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> tenantCodes = records.stream()
                .map(TenantPageVO::getTenantCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        if (tenantCodes.isEmpty()) {
            return;
        }
        String periodKey = YearMonth.now().toString().replace("-", "");
        List<TenantUsageMeter> meters = tenantUsageMeterMapper.selectList(new LambdaQueryWrapper<TenantUsageMeter>()
                .in(TenantUsageMeter::getTenantCode, tenantCodes)
                .eq(TenantUsageMeter::getMeterType, TenantUsageMeterEnum.AI_ADVICE.getCode())
                .eq(TenantUsageMeter::getPeriodKey, periodKey));
        Map<String, Integer> usedMap = meters == null ? Map.of() : meters.stream()
                .collect(Collectors.toMap(
                        TenantUsageMeter::getTenantCode,
                        meter -> meter.getUsedCount() == null ? 0 : meter.getUsedCount(),
                        Math::max
                ));
        records.forEach(record -> record.setAiAdviceUsedThisMonth(usedMap.getOrDefault(record.getTenantCode(), 0)));
    }

    private void initTenantModuleRoles(String tenantCode) {
        List<SysPermission> topPermissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .in(SysPermission::getPermCode, MODULE_ROLE_NAME_MAP.keySet()));

        for (SysPermission permission : topPermissions) {
            SysRole role = new SysRole();
            role.setTenantCode(tenantCode);
            role.setRoleCode(buildRoleCode(permission.getPermCode()));
            role.setRoleName(MODULE_ROLE_NAME_MAP.get(permission.getPermCode()));
            role.setIsSystem(BinaryFlagEnum.YES.getCode());
            role.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            sysRoleMapper.insert(role);

            List<SysPermission> allModulePermissions = collectModulePermissions(permission.getPermCode());
            List<SysRolePermission> mappings = new ArrayList<>();
            for (SysPermission modulePermission : allModulePermissions) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(role.getId());
                relation.setPermissionId(modulePermission.getId());
                relation.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
                mappings.add(relation);
            }
            if (!mappings.isEmpty()) {
                sysRolePermissionMapper.insertBatch(mappings);
            }
        }
    }

    private Long initTenantOwnerRole(String tenantCode) {
        SysRole existed = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getRoleCode, TENANT_OWNER_ROLE_CODE)
                .last("LIMIT 1"));
        if (existed != null) {
            return existed.getId();
        }

        SysRole role = new SysRole();
        role.setTenantCode(tenantCode);
        role.setRoleCode(TENANT_OWNER_ROLE_CODE);
        role.setRoleName("租户负责人");
        role.setIsSystem(BinaryFlagEnum.YES.getCode());
        role.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        sysRoleMapper.insert(role);

        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        List<SysRolePermission> mappings = permissions.stream()
                .map(permission -> {
                    SysRolePermission relation = new SysRolePermission();
                    relation.setRoleId(role.getId());
                    relation.setPermissionId(permission.getId());
                    relation.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
                    return relation;
                })
                .toList();
        if (!mappings.isEmpty()) {
            sysRolePermissionMapper.insertBatch(mappings);
        }
        return role.getId();
    }

    private Long initTenantOwnerUser(String tenantCode, TenantCreateRequest request, Long ownerRoleId) {
        return runIgnoringTenant(() -> {
            String loginName = tenantCode.toLowerCase(Locale.ROOT) + "_admin";
            Employee existed = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getTenantCode, tenantCode)
                    .eq(Employee::getLoginName, loginName)
                    .last("LIMIT 1"));
            if (existed != null) {
                return existed.getId();
            }

            Employee owner = new Employee();
            owner.setTenantCode(tenantCode);
            owner.setName(defaultText(request.getContactPerson(), "租户负责人"));
            owner.setLoginName(loginName);
            owner.setPhone(null);
            owner.setPassword(encryptUtil.encode(request.getPassword() == null || request.getPassword().isBlank()
                    ? defaultTenantPassword
                    : request.getPassword().trim()));
            owner.setDepartmentName("管理部");
            owner.setPosition("负责人");
            owner.setStatus(CommonStatusEnum.ENABLED.getCode());
            owner.setRoleLevel(1);
            employeeMapper.insert(owner);

            EmployeeExt ext = new EmployeeExt();
            ext.setTenantCode(tenantCode);
            ext.setUserId(owner.getId());
            ext.setEmpNo("ADMIN001");
            ext.setEmployeeType("formal");
            ext.setEntryDate(LocalDate.now());
            ext.setRemark("系统创建的租户初始化账号");
            ext.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            employeeExtMapper.insert(ext);

            my.management.module.sys.model.entity.SysUserRole userRole = new my.management.module.sys.model.entity.SysUserRole();
            userRole.setTenantCode(tenantCode);
            userRole.setUserId(owner.getId());
            userRole.setRoleId(ownerRoleId);
            userRole.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            sysUserRoleMapper.insert(userRole);
            return owner.getId();
        });
    }

    private void initTenantOrgDefaults(String tenantCode) {
        runIgnoringTenant(() -> {
            Department management = ensureDepartment(tenantCode, "管理部", "DEPT_MGMT", 1);
            Department sales = ensureDepartment(tenantCode, "销售部", "DEPT_SALES", 2);
            Department warehouse = ensureDepartment(tenantCode, "仓储部", "DEPT_WAREHOUSE", 3);
            ensureDepartment(tenantCode, "生产部", "DEPT_PRODUCTION", 4);
            ensureDepartment(tenantCode, "财务部", "DEPT_FINANCE", 5);
            ensurePosition(tenantCode, management.getId(), "负责人", "POS_OWNER", 1);
            ensurePosition(tenantCode, sales.getId(), "销售员", "POS_SALES", 2);
            ensurePosition(tenantCode, warehouse.getId(), "仓库管理员", "POS_WAREHOUSE", 3);
            return null;
        });
    }

    private Department ensureDepartment(String tenantCode, String name, String code, Integer sortNo) {
        Department existed = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getTenantCode, tenantCode)
                .eq(Department::getDeptCode, code)
                .last("LIMIT 1"));
        if (existed != null) {
            return existed;
        }
        Department department = new Department();
        department.setTenantCode(tenantCode);
        department.setDeptName(name);
        department.setDeptCode(code);
        department.setParentId(0L);
        department.setSortNo(sortNo);
        department.setStatus(CommonStatusEnum.ENABLED.getCode());
        department.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        departmentMapper.insert(department);
        return department;
    }

    private void ensurePosition(String tenantCode, Long departmentId, String name, String code, Integer sortNo) {
        Long count = positionMapper.selectCount(new LambdaQueryWrapper<Position>()
                .eq(Position::getTenantCode, tenantCode)
                .eq(Position::getPositionCode, code));
        if (count != null && count > 0) {
            return;
        }
        Position position = new Position();
        position.setTenantCode(tenantCode);
        position.setDepartmentId(departmentId);
        position.setPositionName(name);
        position.setPositionCode(code);
        position.setSortNo(sortNo);
        position.setStatus(CommonStatusEnum.ENABLED.getCode());
        position.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        positionMapper.insert(position);
    }

    private void initTenantAttendanceRule(String tenantCode, TenantCreateRequest request) {
        TenantAttendanceRule existed = tenantAttendanceRuleManageMapper.selectByTenantCode(tenantCode);
        if (existed != null) {
            return;
        }
        String companyAddress = clean(request.getCompanyAddress());
        Double latitude = request.getCompanyLatitude();
        Double longitude = request.getCompanyLongitude();
        if (!hasValidLocation(latitude, longitude) && companyAddress != null) {
            TiandituGeocodeService.GeocodeResult geocodeResult = tiandituGeocodeService.resolve(request);
            if (geocodeResult.isValid()) {
                latitude = geocodeResult.latitude();
                longitude = geocodeResult.longitude();
            }
        }
        boolean hasValidCompanyLocation = hasValidLocation(latitude, longitude);

        TenantAttendanceRule rule = new TenantAttendanceRule();
        rule.setTenantCode(tenantCode);
        rule.setTenantName(request.getTenantName().trim());
        rule.setStatus(CommonStatusEnum.ENABLED.getCode());
        rule.setLatitude(hasValidCompanyLocation ? latitude : null);
        rule.setLongitude(hasValidCompanyLocation ? longitude : null);
        rule.setRadius(safeAttendanceRadius(request.getAttendanceRadius()));
        rule.setAddress(defaultText(companyAddress, "请在考勤管理中配置打卡地址"));
        rule.setWorkStartTime(LocalTime.of(8, 30));
        rule.setWorkEndTime(LocalTime.of(9, 30));
        rule.setOffWorkStartTime(LocalTime.of(17, 30));
        rule.setOffWorkEndTime(LocalTime.of(18, 30));
        rule.setOverTimeStartTime(LocalTime.of(18, 30));
        rule.setOverTimeEndTime(LocalTime.of(22, 0));
        rule.setLateToleranceMinutes(5);
        rule.setEarlyToleranceMinutes(5);
        rule.setWorkDays("1,2,3,4,5,6");
        // 只有拿到真实公司坐标时才默认启用 GPS，避免空坐标或 0,0 占位导致新租户无法打卡。
        rule.setEnableGps(BinaryFlagEnum.codeOf(hasValidCompanyLocation));
        rule.setEnableWifi(BinaryFlagEnum.NO.getCode());
        tenantAttendanceRuleManageMapper.insert(rule);
    }

    private List<SysPermission> collectModulePermissions(String modulePermCode) {
        String prefix = modulePermCode.substring(0, modulePermCode.length() - 1);
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .and(wrapper -> wrapper.eq(SysPermission::getPermCode, modulePermCode)
                        .or()
                        .likeRight(SysPermission::getPermCode, prefix)))
                .stream()
                .collect(Collectors.toList());
    }

    private String buildRoleCode(String permCode) {
        return "AUTO_" + permCode.replace(":*", "").replace(':', '_').toUpperCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean isValidLatitude(Double value) {
        return value != null && value >= -90D && value <= 90D;
    }

    private boolean isValidLongitude(Double value) {
        return value != null && value >= -180D && value <= 180D;
    }

    private boolean hasValidLocation(Double latitude, Double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    private Double safeAttendanceRadius(Double value) {
        if (value == null || value <= 0D) {
            return 300D;
        }
        return Math.min(value, 10000D);
    }

    private static DefaultRedisScript<Long> buildReleaseLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """);
        return script;
    }

    private <T> T runIgnoringTenant(java.util.concurrent.Callable<T> callable) {
        boolean previous = TenantPermissionContext.isIgnoreTenant();
        TenantPermissionContext.setIgnoreTenant(true);
        try {
            return callable.call();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("租户默认数据初始化失败");
        } finally {
            if (previous) {
                TenantPermissionContext.setIgnoreTenant(true);
            } else {
                TenantPermissionContext.clearIgnore();
            }
        }
    }

    private void ensureDeveloperAccess() {
        if (!developerAccessService.isCurrentUserDeveloper()) {
            throw new BusinessException("仅系统开发者可访问租户管理");
        }
    }
}
