package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.service.DeveloperAccessService;
import my.hive.common.utils.EncryptUtil;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.dto.TenantCreateRequest;
import my.management.module.tenant.model.dto.TenantPageRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.vo.TenantDetailVO;
import my.management.module.tenant.model.vo.TenantPageVO;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * TenantManageService 属于管理端后端租户模块，实现核心业务编排与规则逻辑。
 */
@Service
public class TenantManageService {

    private static final String DEFAULT_TENANT_PASSWORD = "Test@123456";

    private static final Map<String, String> MODULE_ROLE_NAME_MAP = Map.of(
            "document:*", "文档管理员",
            "sales:order:*", "销售订单管理员",
            "production:order:*", "生产订单管理员",
            "inventory:*", "库存管理员",
            "customer:*", "客户管理员",
            "attendance:*", "考勤管理员",
            "approval:*", "审批管理员"
    );

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private DeveloperAccessService developerAccessService;

    public Page<TenantPageVO> page(TenantPageRequest request) {
        ensureDeveloperAccess();
        Page<Tenant> page = new Page<>(request.getCurrent(), request.getSize());
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
        result.setRecords(records);
        return result;
    }

    public TenantDetailVO detail(Long id) {
        ensureDeveloperAccess();
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
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
                ? DEFAULT_TENANT_PASSWORD
                : request.getPassword().trim()));
        tenant.setStatus(1);
        tenant.setDeleted(0);
        tenant.setCreator(TenantPermissionContext.getUserId());
        tenantMapper.insert(tenant);

        initTenantModuleRoles(tenantCode);
        return tenant.getId();
    }

    private TenantPageVO toPageVO(Tenant tenant) {
        TenantPageVO vo = new TenantPageVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }

    private void initTenantModuleRoles(String tenantCode) {
        List<SysPermission> topPermissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, 0)
                .in(SysPermission::getPermCode, MODULE_ROLE_NAME_MAP.keySet()));

        for (SysPermission permission : topPermissions) {
            SysRole role = new SysRole();
            role.setTenantCode(tenantCode);
            role.setRoleCode(buildRoleCode(permission.getPermCode()));
            role.setRoleName(MODULE_ROLE_NAME_MAP.get(permission.getPermCode()));
            role.setIsSystem(1);
            role.setIsDeleted(0);
            sysRoleMapper.insert(role);

            List<SysPermission> allModulePermissions = collectModulePermissions(permission.getPermCode());
            List<SysRolePermission> mappings = new ArrayList<>();
            for (SysPermission modulePermission : allModulePermissions) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(role.getId());
                relation.setPermissionId(modulePermission.getId());
                relation.setIsDeleted(0);
                mappings.add(relation);
            }
            if (!mappings.isEmpty()) {
                sysRolePermissionMapper.insertBatch(mappings);
            }
        }
    }

    private List<SysPermission> collectModulePermissions(String modulePermCode) {
        String prefix = modulePermCode.substring(0, modulePermCode.length() - 1);
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, 0)
                .and(wrapper -> wrapper.eq(SysPermission::getPermCode, modulePermCode)
                        .or()
                        .likeRight(SysPermission::getPermCode, prefix)))
                .stream()
                .collect(Collectors.toList());
    }

    private String buildRoleCode(String permCode) {
        return "AUTO_" + permCode.replace(":*", "").replace(':', '_').toUpperCase(Locale.ROOT);
    }

    private void ensureDeveloperAccess() {
        if (!developerAccessService.isCurrentUserDeveloper()) {
            throw new BusinessException("仅系统开发者可访问租户管理");
        }
    }
}
