package my.hive.domain.permission.service;

import my.hive.shared.permission.PermissionCatalogV3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.enums.DeleteFlagEnum;
import my.hive.domain.permission.mapper.SysPermissionMapper;
import my.hive.domain.permission.mapper.SysRoleMapper;
import my.hive.domain.permission.mapper.SysRolePermissionMapper;
import my.hive.domain.permission.model.entity.SysPermission;
import my.hive.domain.permission.model.entity.SysRole;
import my.hive.domain.permission.model.entity.SysRolePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provisions only missing built-in roles so existing tenant customizations remain intact.
 */
@Service
public class BuiltInRoleProvisionService {

    @Resource
    private BuiltInRoleCatalog builtInRoleCatalog;

    @Resource
    private PermissionCatalogV3 permissionCatalog;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, SysRole> ensureTenantRoles(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("租户编码不能为空");
        }

        List<SysRole> existingRoles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode.trim())
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        LinkedHashMap<String, SysRole> rolesByCode = existingRoles.stream()
                .filter(role -> role != null && StringUtils.hasText(role.getRoleCode()))
                .collect(Collectors.toMap(
                        SysRole::getRoleCode,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        Map<String, SysPermission> permissionsByCode = activePermissionsByCode();
        Set<String> missingCatalogLeaves = permissionCatalog.leaves().stream()
                .filter(code -> !permissionsByCode.containsKey(code))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!missingCatalogLeaves.isEmpty()) {
            throw new BusinessException("权限目录版本不完整，请先执行 V3 数据库迁移");
        }
        for (BuiltInRoleCatalog.RoleDefinition definition : builtInRoleCatalog.definitions()) {
            if (rolesByCode.containsKey(definition.code())) {
                continue;
            }
            SysRole role = createRole(tenantCode.trim(), definition);
            rolesByCode.put(definition.code(), role);
            grantInitialPermissions(role, definition, permissionsByCode);
        }
        return Collections.unmodifiableMap(rolesByCode);
    }

    private Map<String, SysPermission> activePermissionsByCode() {
        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getAssignable, 1));
        return permissions.stream()
                .filter(permission -> permission != null && StringUtils.hasText(permission.getPermCode()))
                .collect(Collectors.toMap(
                        SysPermission::getPermCode,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private SysRole createRole(String tenantCode, BuiltInRoleCatalog.RoleDefinition definition) {
        SysRole role = new SysRole();
        role.setTenantCode(tenantCode);
        role.setRoleCode(definition.code());
        role.setRoleName(definition.name());
        role.setIsSystem(1);
        role.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        sysRoleMapper.insert(role);
        return role;
    }

    private void grantInitialPermissions(SysRole role,
                                         BuiltInRoleCatalog.RoleDefinition definition,
                                         Map<String, SysPermission> permissionsByCode) {
        Set<String> allowedCodes = definition.permissions();
        List<SysRolePermission> grants = new ArrayList<>();
        for (String permissionCode : allowedCodes) {
            SysPermission permission = permissionsByCode.get(permissionCode);
            SysRolePermission grant = new SysRolePermission();
            grant.setRoleId(role.getId());
            grant.setPermissionId(permission.getId());
            grant.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            grants.add(grant);
        }
        if (!grants.isEmpty()) {
            sysRolePermissionMapper.upsertBatch(grants);
        }
    }
}
