package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.utils.EncryptUtil;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.storage.BusinessAttachmentService;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.employee.mapper.DepartmentMapper;
import my.management.module.employee.mapper.EmployeeExtMapper;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.mapper.PositionMapper;
import my.management.module.employee.model.entity.Department;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.entity.EmployeeExt;
import my.management.module.employee.model.entity.Position;
import my.management.module.employee.model.enums.EmployeeStatusEnum;
import my.management.module.employee.model.enums.EmployeeTypeEnum;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysUserRole;
import my.management.module.sys.service.BuiltInRoleProvisionService;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.dto.TenantOwnerAccountRequest;
import my.management.module.tenant.model.dto.TenantProfileUpdateRequest;
import my.management.module.tenant.model.dto.TenantStatusUpdateRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.vo.TenantFeatureOptionVO;
import my.management.module.tenant.model.vo.TenantManageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.Locale;

@Service
public class TenantManageService {

    private static final String OWNER_ROLE_CODE = "ADMIN";
    private static final String OWNER_ROLE_NAME = "企业负责人";
    private static final String EMPLOYEE_ROLE_CODE = "EMPLOYEE";
    private static final String EMPLOYEE_ROLE_NAME = "普通员工";
    private static final String OWNER_DEPARTMENT_NAME = "管理部";
    private static final String OWNER_POSITION_NAME = "企业负责人";
    private static final int OWNER_ROLE_LEVEL = 3;
    private static final int EMPLOYEE_ROLE_LEVEL = 0;
    private static final int MAX_TENANT_NAME_LENGTH = 80;
    private static final int MAX_CONTACT_LENGTH = 50;
    private static final int MAX_CONTACT_PHONE_LENGTH = 30;
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> LOGO_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final int MAX_OWNER_NAME_LENGTH = 50;
    private static final int MAX_LOGIN_NAME_LENGTH = 64;
    private static final int MAX_PHONE_LENGTH = 20;
    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private EmployeeExtMapper employeeExtMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private PrivacyProtectionUtil privacyProtectionUtil;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    @Resource
    private BuiltInRoleProvisionService builtInRoleProvisionService;

    @Resource
    private BusinessAttachmentService businessAttachmentService;

    public List<TenantManageVO> list() {
        return tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getDeleted, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(Tenant::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<TenantFeatureOptionVO> featureCatalog() {
        return tenantLicenseService.featureCatalog();
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantManageVO updateProfile(Long id, TenantProfileUpdateRequest request) {
        if (id == null || id <= 0 || request == null) {
            throw new BusinessException("企业信息参数不完整");
        }
        Tenant tenant = requireTenant(id);
        tenant.setTenantName(requireText(request.getTenantName(), "企业名称不能为空", MAX_TENANT_NAME_LENGTH));
        tenant.setTenantType(request.getTenantType());
        tenant.setContactPerson(optionalText(request.getContactPerson(), MAX_CONTACT_LENGTH, "联系人"));
        tenant.setContactPhone(optionalText(request.getContactPhone(), MAX_CONTACT_PHONE_LENGTH, "联系电话"));
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        tenantLicenseService.clearTenantRuntimeCache(tenant.getTenantCode());
        return toVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantManageVO uploadLogo(Long id, MultipartFile file) {
        if (id == null || id <= 0) {
            throw new BusinessException("企业信息参数不完整");
        }
        Tenant tenant = requireTenant(id);
        validateLogoFile(file);
        BusinessAttachmentVO attachment = withTenantContext(tenant.getTenantCode(), () ->
                businessAttachmentService.upload(file, "tenant-logo")
        );
        tenant.setLogoUrl(attachment.getFileUrl());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        tenantLicenseService.clearTenantRuntimeCache(tenant.getTenantCode());
        return toVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantManageVO updateLicense(Long id, TenantLicenseUpdateRequest request) {
        if (id == null || id <= 0 || request == null) {
            throw new BusinessException("租户授权参数不完整");
        }
        Tenant tenant = requireTenant(id);
        request.setId(id);
        tenantLicenseService.applyLicenseUpdate(tenant, request);
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        tenantLicenseService.clearTenantRuntimeCache(tenant.getTenantCode());
        return toVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantManageVO updateStatus(Long id, TenantStatusUpdateRequest request) {
        if (id == null || id <= 0 || request == null || request.getStatus() == null) {
            throw new BusinessException("租户状态参数不完整");
        }
        if (!CommonStatusEnum.isEnabled(request.getStatus()) && request.getStatus() != 0) {
            throw new BusinessException("租户状态不合法");
        }
        Tenant tenant = requireTenant(id);
        tenant.setStatus(request.getStatus());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        tenantLicenseService.clearTenantRuntimeCache(tenant.getTenantCode());
        return toVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantManageVO reassignOwnerAccount(Long id, TenantOwnerAccountRequest request) {
        if (id == null || id <= 0 || request == null) {
            throw new BusinessException("负责人账号参数不完整");
        }
        Tenant tenant = requireTenant(id);
        withTenantContext(tenant.getTenantCode(), () -> {
            Department department = getOrCreateOwnerDepartment(tenant.getTenantCode(), request.getOwnerName());
            Position position = getOrCreateOwnerPosition(tenant.getTenantCode(), department.getId());
            Map<String, SysRole> builtInRoles = builtInRoleProvisionService.ensureTenantRoles(tenant.getTenantCode());
            SysRole ownerRole = builtInRoles.get(OWNER_ROLE_CODE);
            SysRole employeeRole = builtInRoles.get(EMPLOYEE_ROLE_CODE);
            Employee owner = resolveOrCreateOwnerEmployee(tenant.getTenantCode(), request, department, position);
            if (bindUserRole(owner.getId(), tenant.getTenantCode(), ownerRole.getId())) {
                employeeMapper.incrementPermissionVersion(tenant.getTenantCode(), owner.getId());
            }
            reassignOwnerRole(tenant.getTenantCode(), owner.getId(), ownerRole.getId(), employeeRole.getId());
            permissionCacheUtil.evict(tenant.getTenantCode(), owner.getId());
            return owner;
        });
        tenant.setContactPerson(clean(request.getOwnerName()));
        String normalizedPhone = privacyProtectionUtil.normalizePhone(request.getPhone());
        if (StringUtils.hasText(normalizedPhone)) {
            tenant.setContactPhone(privacyProtectionUtil.maskPhone(normalizedPhone));
        }
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
        tenantLicenseService.clearTenantRuntimeCache(tenant.getTenantCode());
        return toVO(tenant);
    }

    private Tenant requireTenant(Long id) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getId, id)
                .eq(Tenant::getDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        return tenant;
    }

    private Employee resolveOrCreateOwnerEmployee(String tenantCode,
                                                  TenantOwnerAccountRequest request,
                                                  Department department,
                                                  Position position) {
        String ownerName = requireText(request.getOwnerName(), "负责人姓名不能为空", MAX_OWNER_NAME_LENGTH);
        String loginName = clean(request.getLoginName());
        String normalizedPhone = privacyProtectionUtil.normalizePhone(request.getPhone());
        if (StringUtils.hasText(request.getPhone()) && !StringUtils.hasText(normalizedPhone)) {
            throw new BusinessException("负责人手机号格式不正确");
        }
        if (StringUtils.hasText(normalizedPhone) && normalizedPhone.length() > MAX_PHONE_LENGTH) {
            throw new BusinessException("负责人手机号长度不能超过20位");
        }
        if (!StringUtils.hasText(loginName) && StringUtils.hasText(normalizedPhone)) {
            loginName = normalizedPhone;
        }
        loginName = requireText(loginName, "负责人登录账号或手机号至少填写一个", MAX_LOGIN_NAME_LENGTH);
        String initialPassword = requirePassword(request.getInitialPassword());

        Employee target = findTargetOwnerEmployee(tenantCode, request.getOwnerUserId(), loginName, normalizedPhone);
        ensureLoginNameAvailable(target, loginName, tenantCode);
        ensurePhoneAvailable(target, normalizedPhone, tenantCode);

        Employee owner = target == null ? new Employee() : target;
        owner.setTenantCode(tenantCode);
        owner.setName(ownerName);
        owner.setLoginName(loginName);
        if (StringUtils.hasText(normalizedPhone)) {
            owner.setPhone(privacyProtectionUtil.maskPhone(normalizedPhone));
            owner.setPhoneHash(privacyProtectionUtil.hashPhone(normalizedPhone));
            owner.setPhoneMask(privacyProtectionUtil.maskPhone(normalizedPhone));
        }
        owner.setPassword(encryptUtil.encode(initialPassword));
        owner.setMustChangePassword(1);
        owner.setDepartmentName(department.getDeptName());
        owner.setPosition(position.getPositionName());
        owner.setManagerId(null);
        owner.setManagerName(null);
        owner.setStatus(EmployeeStatusEnum.ACTIVE.getCode());
        owner.setAttendanceRequired(Boolean.TRUE.equals(request.getAttendanceRequired()) ? 1 : 0);
        owner.setRoleLevel(OWNER_ROLE_LEVEL);
        if (owner.getId() == null) {
            owner.setPermissionVersion(1L);
            owner.setAuthVersion(1L);
            employeeMapper.insert(owner);
        } else {
            employeeMapper.updateById(owner);
            employeeMapper.incrementAuthVersion(tenantCode, owner.getId());
        }
        upsertOwnerEmployeeExt(owner.getId(), tenantCode);
        return owner;
    }

    private Employee findTargetOwnerEmployee(String tenantCode, Long ownerUserId, String loginName, String normalizedPhone) {
        if (ownerUserId != null && ownerUserId > 0) {
            Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getId, ownerUserId)
                    .eq(Employee::getTenantCode, tenantCode)
                    .last("LIMIT 1"));
            if (employee == null) {
                throw new BusinessException("指定的负责人账号不存在");
            }
            return employee;
        }
        if (StringUtils.hasText(loginName)) {
            Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getTenantCode, tenantCode)
                    .eq(Employee::getLoginName, loginName)
                    .last("LIMIT 1"));
            if (employee != null) {
                return employee;
            }
        }
        if (StringUtils.hasText(normalizedPhone)) {
            String phoneHash = privacyProtectionUtil.hashPhone(normalizedPhone);
            return employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getTenantCode, tenantCode)
                    .eq(Employee::getPhoneHash, phoneHash)
                    .last("LIMIT 1"));
        }
        return null;
    }

    private void ensureLoginNameAvailable(Employee target, String loginName, String tenantCode) {
        if (!StringUtils.hasText(loginName)) {
            return;
        }
        List<Employee> matches = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getLoginName, loginName)
                .last("LIMIT 10"));
        for (Employee match : matches) {
            if (match == null || (target != null && match.getId().equals(target.getId()))) {
                continue;
            }
            if (!tenantCode.equals(match.getTenantCode())) {
                throw new BusinessException("该登录账号已属于其他企业，不能作为本企业负责人账号");
            }
            throw new BusinessException("该登录账号已属于本企业其他员工");
        }
    }

    private void ensurePhoneAvailable(Employee target, String normalizedPhone, String tenantCode) {
        if (!StringUtils.hasText(normalizedPhone)) {
            return;
        }
        String phoneHash = privacyProtectionUtil.hashPhone(normalizedPhone);
        List<Employee> matches = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getPhoneHash, phoneHash)
                .last("LIMIT 10"));
        for (Employee match : matches) {
            if (match == null || (target != null && match.getId().equals(target.getId()))) {
                continue;
            }
            if (!tenantCode.equals(match.getTenantCode())) {
                throw new BusinessException("该手机号已属于其他企业，不能作为本企业负责人手机号");
            }
            throw new BusinessException("该手机号已属于本企业其他员工");
        }
    }

    private void upsertOwnerEmployeeExt(Long userId, String tenantCode) {
        EmployeeExt ext = employeeExtMapper.selectOne(new LambdaQueryWrapper<EmployeeExt>()
                .eq(EmployeeExt::getUserId, userId)
                .last("LIMIT 1"));
        if (ext == null) {
            ext = new EmployeeExt();
            ext.setUserId(userId);
            ext.setTenantCode(tenantCode);
            ext.setEmpNo(codeGeneratorUtil.generateEmployeeNo());
            ext.setEmployeeType(EmployeeTypeEnum.FULL_TIME.getCode());
            ext.setEntryDate(LocalDate.now());
            ext.setRemark("企业负责人账号");
            ext.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            employeeExtMapper.insert(ext);
            return;
        }
        if (!StringUtils.hasText(ext.getEmpNo())) {
            ext.setEmpNo(codeGeneratorUtil.generateEmployeeNo());
        }
        ext.setEmployeeType(EmployeeTypeEnum.FULL_TIME.getCode());
        if (ext.getEntryDate() == null) {
            ext.setEntryDate(LocalDate.now());
        }
        ext.setRemark("企业负责人账号");
        ext.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        employeeExtMapper.updateById(ext);
    }

    private Department getOrCreateOwnerDepartment(String tenantCode, String leaderName) {
        Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getTenantCode, tenantCode)
                .eq(Department::getDeptName, OWNER_DEPARTMENT_NAME)
                .eq(Department::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (department != null) {
            department.setLeaderName(clean(leaderName));
            departmentMapper.updateById(department);
            return department;
        }
        Department created = new Department();
        created.setTenantCode(tenantCode);
        created.setDeptName(OWNER_DEPARTMENT_NAME);
        created.setDeptCode(codeGeneratorUtil.generateCode("DPT", 4));
        created.setParentId(null);
        created.setLeaderName(clean(leaderName));
        created.setSortNo(1);
        created.setStatus(CommonStatusEnum.ENABLED.getCode());
        created.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        departmentMapper.insert(created);
        return created;
    }

    private Position getOrCreateOwnerPosition(String tenantCode, Long departmentId) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getTenantCode, tenantCode)
                .eq(Position::getPositionName, OWNER_POSITION_NAME)
                .eq(Position::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (position != null) {
            position.setDepartmentId(departmentId);
            positionMapper.updateById(position);
            return position;
        }
        Position created = new Position();
        created.setTenantCode(tenantCode);
        created.setPositionName(OWNER_POSITION_NAME);
        created.setPositionCode(codeGeneratorUtil.generateCode("POS", 4));
        created.setDepartmentId(departmentId);
        created.setSortNo(1);
        created.setStatus(CommonStatusEnum.ENABLED.getCode());
        created.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        positionMapper.insert(created);
        return created;
    }

    private boolean bindUserRole(Long userId, String tenantCode, Long roleId) {
        SysUserRole existing = sysUserRoleMapper.selectOne(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantCode, tenantCode)
                .eq(SysUserRole::getRoleId, roleId)
                .eq(SysUserRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (existing != null) {
            return false;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setTenantCode(tenantCode);
        userRole.setRoleId(roleId);
        userRole.setCreateTime(LocalDateTime.now());
        userRole.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        sysUserRoleMapper.insert(userRole);
        return true;
    }

    private void reassignOwnerRole(String tenantCode, Long ownerUserId, Long ownerRoleId, Long employeeRoleId) {
        List<SysUserRole> ownerBindings = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getTenantCode, tenantCode)
                .eq(SysUserRole::getRoleId, ownerRoleId)
                .eq(SysUserRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        for (SysUserRole binding : ownerBindings) {
            if (binding == null || ownerUserId.equals(binding.getUserId())) {
                continue;
            }
            binding.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
            sysUserRoleMapper.updateById(binding);
            bindUserRole(binding.getUserId(), tenantCode, employeeRoleId);
            downgradePreviousOwner(binding.getUserId(), tenantCode);
            employeeMapper.incrementPermissionVersion(tenantCode, binding.getUserId());
            permissionCacheUtil.evict(tenantCode, binding.getUserId());
        }
    }

    private void downgradePreviousOwner(Long userId, String tenantCode) {
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getId, userId)
                .eq(Employee::getTenantCode, tenantCode)
                .last("LIMIT 1"));
        if (employee == null) {
            return;
        }
        if (employee.getRoleLevel() == null || employee.getRoleLevel() >= OWNER_ROLE_LEVEL) {
            employee.setRoleLevel(EMPLOYEE_ROLE_LEVEL);
            if (OWNER_POSITION_NAME.equals(employee.getPosition())) {
                employee.setPosition(EMPLOYEE_ROLE_NAME);
            }
            employeeMapper.updateById(employee);
        }
    }

    private Employee findCurrentOwner(String tenantCode) {
        SysRole ownerRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getRoleCode, OWNER_ROLE_CODE)
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (ownerRole != null) {
            List<SysUserRole> bindings = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getTenantCode, tenantCode)
                    .eq(SysUserRole::getRoleId, ownerRole.getId())
                    .eq(SysUserRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
            Set<Long> userIds = new HashSet<>();
            for (SysUserRole binding : bindings) {
                if (binding != null && binding.getUserId() != null) {
                    userIds.add(binding.getUserId());
                }
            }
            if (!userIds.isEmpty()) {
                return employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getTenantCode, tenantCode)
                        .in(Employee::getId, userIds)
                        .eq(Employee::getStatus, EmployeeStatusEnum.ACTIVE.getCode())
                        .orderByDesc(Employee::getRoleLevel)
                        .orderByAsc(Employee::getId)
                        .last("LIMIT 1"));
            }
        }
        return employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, tenantCode)
                .eq(Employee::getStatus, EmployeeStatusEnum.ACTIVE.getCode())
                .orderByDesc(Employee::getRoleLevel)
                .orderByAsc(Employee::getId)
                .last("LIMIT 1"));
    }

    private TenantManageVO toVO(Tenant tenant) {
        TenantManageVO vo = new TenantManageVO();
        vo.setId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getTenantName());
        vo.setLogoUrl(tenant.getLogoUrl());
        vo.setTenantType(tenant.getTenantType());
        vo.setContactPerson(tenant.getContactPerson());
        vo.setContactPhone(tenant.getContactPhone());
        vo.setStatus(tenant.getStatus());
        vo.setPackageCode(tenant.getPackageCode());
        vo.setPackageName(tenant.getPackageName());
        vo.setSubscriptionStatus(tenant.getSubscriptionStatus());
        vo.setSubscriptionStartTime(tenant.getSubscriptionStartTime());
        vo.setSubscriptionEndTime(tenant.getSubscriptionEndTime());
        vo.setMaxUsers(tenant.getMaxUsers());
        vo.setMaxStorageMb(tenant.getMaxStorageMb());
        vo.setFeatureFlags(tenant.getFeatureFlags());
        vo.setEnabledFeatures(tenantLicenseService.enabledFeatureKeys(tenant.getTenantCode()));
        withTenantContext(tenant.getTenantCode(), () -> {
            Employee owner = findCurrentOwner(tenant.getTenantCode());
            if (owner != null) {
                vo.setOwnerUserId(owner.getId());
                vo.setOwnerName(owner.getName());
                vo.setOwnerLoginName(owner.getLoginName());
                vo.setOwnerPhone(privacyProtectionUtil.displayPhone(owner.getPhone(), owner.getPhoneMask()));
                vo.setOwnerAttendanceRequired(owner.getAttendanceRequired());
            }
            return null;
        });
        vo.setCreateTime(tenant.getCreateTime());
        vo.setUpdateTime(tenant.getUpdateTime());
        return vo;
    }

    private <T> T withTenantContext(String tenantCode, Supplier<T> supplier) {
        String previousTenantCode = TenantPermissionContext.getTenantCode();
        Long previousUserId = TenantPermissionContext.getUserId();
        Set<String> previousPermissions = new HashSet<>(TenantPermissionContext.getPermCodes());
        TenantPermissionContext.init(tenantCode, previousUserId, previousPermissions);
        try {
            return supplier.get();
        } finally {
            if (StringUtils.hasText(previousTenantCode) || previousUserId != null || !previousPermissions.isEmpty()) {
                TenantPermissionContext.init(previousTenantCode, previousUserId, previousPermissions);
            } else {
                TenantPermissionContext.clear();
            }
        }
    }

    private String requireText(String value, String message, int maxLength) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new BusinessException(message);
        }
        if (cleaned.length() > maxLength) {
            throw new BusinessException(message.replace("不能为空", "长度不能超过" + maxLength + "位"));
        }
        return cleaned;
    }

    private String requirePassword(String value) {
        String password = clean(value);
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("初始密码不能为空");
        }
        if (password.length() < 6 || password.length() > 64) {
            throw new BusinessException("初始密码长度需为6-64位");
        }
        return password;
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的公司Logo");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BusinessException("公司Logo不能超过2MB");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!LOGO_EXTENSIONS.contains(normalizedExtension)) {
            throw new BusinessException("公司Logo仅支持 PNG、JPG、JPEG、WEBP 图片");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("公司Logo必须是图片文件");
        }
    }

    private String optionalText(String value, int maxLength, String fieldName) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        if (cleaned.length() > maxLength) {
            throw new BusinessException(fieldName + "长度不能超过" + maxLength + "位");
        }
        return cleaned;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
