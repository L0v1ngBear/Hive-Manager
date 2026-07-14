package my.management.module.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.mapper.EmployeeChangeLogMapper;
import my.management.module.employee.model.dto.EmployeePermissionOverrideRequest;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.entity.EmployeeChangeLog;
import my.management.module.employee.model.enums.EmployeeStatusEnum;
import my.management.module.employee.model.vo.EmployeePermissionNodeVO;
import my.management.module.employee.model.vo.EmployeePermissionProfileVO;
import my.management.module.employee.model.vo.EmployeePermissionRoleSourceVO;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.mapper.SysUserPermissionMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import my.management.module.sys.model.entity.SysUserPermission;
import my.management.module.sys.service.PermissionCatalogV3;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EmployeePermissionProfileService {

    private static final String SOURCE_NONE = "NONE";
    private static final String SOURCE_ROLE = "ROLE";
    private static final String SOURCE_PERSONAL_GRANT = "PERSONAL_GRANT";
    private static final String SOURCE_PERSONAL_DENY = "PERSONAL_DENY";
    private static final String SOURCE_DESCENDANT = "DESCENDANT";

    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    @Resource
    private SysPermissionMapper sysPermissionMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;
    @Resource
    private SysUserPermissionMapper sysUserPermissionMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private PermissionCacheUtil permissionCacheUtil;
    @Resource
    private PermissionCatalogV3 permissionCatalog;
    @Resource
    private ObjectMapper objectMapper;

    public EmployeePermissionProfileVO profile(Long userId) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Employee employee = requireEmployee(tenantCode, userId);
        List<SysPermission> permissions = selectCatalogPermissions();
        Map<Long, SysPermission> permissionById = indexPermissions(permissions);
        List<SysRole> roles = selectUserRoles(tenantCode, userId);
        Map<Long, EmployeePermissionRoleSourceVO> roleSourceById = indexRoleSources(roles);
        Map<Long, List<EmployeePermissionRoleSourceVO>> permissionRoleSources =
                selectPermissionRoleSources(roles, roleSourceById);
        Map<Long, String> personalEffects = selectPersonalEffects(tenantCode, userId, permissionById.keySet());

        EmployeePermissionProfileVO profile = new EmployeePermissionProfileVO();
        profile.setUserId(userId);
        profile.setPermissionVersion(employee.getPermissionVersion());
        profile.setRoles(new ArrayList<>(roleSourceById.values()));
        profile.setGrants(effectCodes(personalEffects, permissionById, SysUserPermission.EFFECT_GRANT));
        profile.setDenies(effectCodes(personalEffects, permissionById, SysUserPermission.EFFECT_DENY));
        profile.setPermissions(buildTree(permissions, permissionRoleSources, personalEffects));
        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public long updateOverrides(Long userId, EmployeePermissionOverrideRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Employee employee = requireEmployee(tenantCode, userId);
        long expectedVersion = request.getPermissionVersion();
        if (!Objects.equals(employee.getPermissionVersion(), expectedVersion)) {
            throw new BusinessException(409, "员工权限已被其他人修改，请刷新后重试");
        }

        Set<String> grants = normalizeCodes(request.getGrants());
        Set<String> denies = normalizeCodes(request.getDenies());
        Set<String> overlap = new LinkedHashSet<>(grants);
        overlap.retainAll(denies);
        if (!overlap.isEmpty()) {
            throw new BusinessException("同一权限不能同时设置为允许和禁用");
        }
        validateAssignableCodes(grants);
        validateAssignableCodes(denies);

        Map<String, SysPermission> permissionByCode = indexPermissionsByCode(selectCatalogPermissions());
        Map<Long, SysPermission> permissionById = indexPermissions(new ArrayList<>(permissionByCode.values()));
        Set<String> requestedCodes = new LinkedHashSet<>(grants);
        requestedCodes.addAll(denies);
        if (!permissionByCode.keySet().containsAll(requestedCodes)) {
            throw new BusinessException(500, "权限目录未初始化完整，请先执行部署迁移");
        }
        Map<Long, String> previousEffects = selectPersonalEffects(tenantCode, userId, permissionById.keySet());

        if (employeeMapper.incrementPermissionVersionIfCurrent(tenantCode, userId, expectedVersion) != 1) {
            throw new BusinessException(409, "员工权限已被其他人修改，请刷新后重试");
        }
        sysUserPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>()
                .eq(SysUserPermission::getTenantCode, tenantCode)
                .eq(SysUserPermission::getUserId, userId));

        List<SysUserPermission> rows = new ArrayList<>();
        grants.forEach(code -> rows.add(buildOverride(
                tenantCode, userId, permissionByCode.get(code).getId(), SysUserPermission.EFFECT_GRANT)));
        denies.forEach(code -> rows.add(buildOverride(
                tenantCode, userId, permissionByCode.get(code).getId(), SysUserPermission.EFFECT_DENY)));
        if (!rows.isEmpty()) {
            sysUserPermissionMapper.upsertBatch(rows);
        }
        insertChangeLog(userId,
                overrideSnapshot(previousEffects, permissionById),
                Map.of("grants", grants, "denies", denies, "permissionVersion", expectedVersion + 1));
        permissionCacheUtil.evict(tenantCode, userId);
        return expectedVersion + 1;
    }

    private Employee requireEmployee(String tenantCode, Long userId) {
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, tenantCode)
                .eq(Employee::getId, userId)
                .last("LIMIT 1"));
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        if (!isPermissionProfileEligible(employee.getStatus())) {
            throw new BusinessException(403, "仅在职或试用员工允许查看和配置个人权限");
        }
        if (employee.getPermissionVersion() == null) {
            throw new BusinessException(500, "员工权限版本未初始化，请先执行部署迁移");
        }
        return employee;
    }

    private boolean isPermissionProfileEligible(Integer status) {
        return EmployeeStatusEnum.ACTIVE.getCode().equals(status)
                || EmployeeStatusEnum.PROBATION.getCode().equals(status);
    }

    private List<SysPermission> selectCatalogPermissions() {
        List<SysPermission> rows = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
        List<SysPermission> catalogRows = rows == null ? List.of() : rows.stream()
                .filter(permission -> permission != null && permissionCatalog.contains(permission.getPermCode()))
                .toList();
        Set<String> codes = new LinkedHashSet<>();
        catalogRows.forEach(permission -> codes.add(permission.getPermCode()));
        Set<String> expectedCodes = permissionCatalog.definitions().stream()
                .map(PermissionCatalogV3.PermissionDefinition::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!codes.equals(expectedCodes)) {
            throw new BusinessException(500, "权限目录未初始化完整，请先执行部署迁移");
        }
        return catalogRows;
    }

    private List<SysRole> selectUserRoles(String tenantCode, Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserIdAndTenantCode(userId, tenantCode);
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .orderByAsc(SysRole::getId));
        return roles == null ? List.of() : roles;
    }

    private Map<Long, EmployeePermissionRoleSourceVO> indexRoleSources(List<SysRole> roles) {
        Map<Long, EmployeePermissionRoleSourceVO> result = new LinkedHashMap<>();
        for (SysRole role : roles) {
            if (role == null || role.getId() == null) {
                continue;
            }
            EmployeePermissionRoleSourceVO source = new EmployeePermissionRoleSourceVO();
            source.setRoleId(role.getId());
            source.setRoleCode(role.getRoleCode());
            source.setRoleName(role.getRoleName());
            result.put(role.getId(), source);
        }
        return result;
    }

    private Map<Long, List<EmployeePermissionRoleSourceVO>> selectPermissionRoleSources(
            List<SysRole> roles,
            Map<Long, EmployeePermissionRoleSourceVO> roleSourceById) {
        if (roles.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).filter(Objects::nonNull).toList();
        List<SysRolePermission> relations = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds)
                        .eq(SysRolePermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        Map<Long, List<EmployeePermissionRoleSourceVO>> result = new LinkedHashMap<>();
        if (relations == null) {
            return result;
        }
        for (SysRolePermission relation : relations) {
            EmployeePermissionRoleSourceVO source = roleSourceById.get(relation.getRoleId());
            if (source == null || relation.getPermissionId() == null) {
                continue;
            }
            result.computeIfAbsent(relation.getPermissionId(), ignored -> new ArrayList<>()).add(source);
        }
        result.values().forEach(sources -> sources.sort(Comparator.comparing(
                EmployeePermissionRoleSourceVO::getRoleId, Comparator.nullsLast(Long::compareTo))));
        return result;
    }

    private Map<Long, String> selectPersonalEffects(String tenantCode,
                                                     Long userId,
                                                     Collection<Long> catalogPermissionIds) {
        List<SysUserPermission> relations = sysUserPermissionMapper.selectList(
                new LambdaQueryWrapper<SysUserPermission>()
                        .eq(SysUserPermission::getTenantCode, tenantCode)
                        .eq(SysUserPermission::getUserId, userId)
                        .eq(SysUserPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(SysUserPermission::getPermissionId));
        Map<Long, String> result = new LinkedHashMap<>();
        if (relations == null) {
            return result;
        }
        for (SysUserPermission relation : relations) {
            if (relation == null || !catalogPermissionIds.contains(relation.getPermissionId())) {
                continue;
            }
            String effect = relation.getEffect();
            if (!SysUserPermission.EFFECT_GRANT.equals(effect) && !SysUserPermission.EFFECT_DENY.equals(effect)) {
                continue;
            }
            if (SysUserPermission.EFFECT_DENY.equals(effect)
                    || !SysUserPermission.EFFECT_DENY.equals(result.get(relation.getPermissionId()))) {
                result.put(relation.getPermissionId(), effect);
            }
        }
        return result;
    }

    private Set<String> effectCodes(Map<Long, String> effects,
                                    Map<Long, SysPermission> permissionById,
                                    String expectedEffect) {
        Set<String> result = new LinkedHashSet<>();
        effects.forEach((permissionId, effect) -> {
            SysPermission permission = permissionById.get(permissionId);
            if (expectedEffect.equals(effect) && permission != null
                    && permissionCatalog.isAssignable(permission.getPermCode())) {
                result.add(permission.getPermCode());
            }
        });
        return result;
    }

    private List<EmployeePermissionNodeVO> buildTree(
            List<SysPermission> permissions,
            Map<Long, List<EmployeePermissionRoleSourceVO>> permissionRoleSources,
            Map<Long, String> personalEffects) {
        Map<Long, EmployeePermissionNodeVO> nodeById = new LinkedHashMap<>();
        Map<EmployeePermissionNodeVO, Long> idByNode = new IdentityHashMap<>();
        Map<Long, List<EmployeePermissionNodeVO>> childrenByParent = new LinkedHashMap<>();
        List<EmployeePermissionNodeVO> roots = new ArrayList<>();
        for (SysPermission permission : permissions) {
            EmployeePermissionNodeVO node = toNode(permission, permissionRoleSources, personalEffects);
            nodeById.put(permission.getId(), node);
            idByNode.put(node, permission.getId());
        }
        for (SysPermission permission : permissions) {
            EmployeePermissionNodeVO node = nodeById.get(permission.getId());
            Long parentId = permission.getParentId();
            if (parentId == null || parentId == 0L || !nodeById.containsKey(parentId)) {
                roots.add(node);
            } else {
                childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(node);
            }
        }
        roots.forEach(root -> attachChildren(idByNode.get(root), root, childrenByParent, idByNode));
        sortTree(roots, idByNode);
        return roots;
    }

    private EmployeePermissionNodeVO toNode(
            SysPermission permission,
            Map<Long, List<EmployeePermissionRoleSourceVO>> permissionRoleSources,
            Map<Long, String> personalEffects) {
        EmployeePermissionNodeVO node = new EmployeePermissionNodeVO();
        node.setCode(permission.getPermCode());
        node.setName(permission.getPermName());
        node.setModuleCode(permission.getModuleCode());
        node.setType(permission.getPermType());
        node.setSort(permission.getSort());
        node.setAssignable(Integer.valueOf(1).equals(permission.getAssignable()));
        List<EmployeePermissionRoleSourceVO> roleSources = new ArrayList<>(
                permissionRoleSources.getOrDefault(permission.getId(), List.of()));
        node.setRoleSources(roleSources);
        node.setRoleGranted(!roleSources.isEmpty());
        String personalEffect = personalEffects.get(permission.getId());
        node.setPersonalEffect(personalEffect);
        applyEffectiveState(node, personalEffect);
        return node;
    }

    private void applyEffectiveState(EmployeePermissionNodeVO node, String personalEffect) {
        if (SysUserPermission.EFFECT_DENY.equals(personalEffect)) {
            node.setEffective(false);
            node.setEffectiveSource(SOURCE_PERSONAL_DENY);
        } else if (SysUserPermission.EFFECT_GRANT.equals(personalEffect)) {
            node.setEffective(true);
            node.setEffectiveSource(SOURCE_PERSONAL_GRANT);
        } else if (Boolean.TRUE.equals(node.getRoleGranted())) {
            node.setEffective(true);
            node.setEffectiveSource(SOURCE_ROLE);
        } else {
            node.setEffective(false);
            node.setEffectiveSource(SOURCE_NONE);
        }
    }

    private void attachChildren(Long permissionId,
                                EmployeePermissionNodeVO node,
                                Map<Long, List<EmployeePermissionNodeVO>> childrenByParent,
                                Map<EmployeePermissionNodeVO, Long> idByNode) {
        List<EmployeePermissionNodeVO> children = childrenByParent.getOrDefault(permissionId, Collections.emptyList());
        node.setChildren(new ArrayList<>(children));
        node.getChildren().forEach(child -> attachChildren(idByNode.get(child), child, childrenByParent, idByNode));
        if (!Boolean.TRUE.equals(node.getAssignable()) && !node.getChildren().isEmpty()) {
            node.setEffective(node.getChildren().stream().anyMatch(child -> Boolean.TRUE.equals(child.getEffective())));
            node.setRoleGranted(node.getChildren().stream().anyMatch(child -> Boolean.TRUE.equals(child.getRoleGranted())));
            node.setEffectiveSource(Boolean.TRUE.equals(node.getEffective()) ? SOURCE_DESCENDANT : SOURCE_NONE);
        }
    }

    private void sortTree(List<EmployeePermissionNodeVO> nodes,
                          Map<EmployeePermissionNodeVO, Long> idByNode) {
        nodes.sort(Comparator.comparing(EmployeePermissionNodeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(idByNode::get, Comparator.nullsLast(Long::compareTo)));
        nodes.forEach(node -> sortTree(node.getChildren(), idByNode));
    }

    private Map<Long, SysPermission> indexPermissions(List<SysPermission> permissions) {
        Map<Long, SysPermission> result = new LinkedHashMap<>();
        permissions.forEach(permission -> result.put(permission.getId(), permission));
        return result;
    }

    private Map<String, SysPermission> indexPermissionsByCode(List<SysPermission> permissions) {
        Map<String, SysPermission> result = new LinkedHashMap<>();
        permissions.forEach(permission -> result.put(permission.getPermCode(), permission));
        return result;
    }

    private Set<String> normalizeCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> result = new LinkedHashSet<>();
        codes.stream().filter(Objects::nonNull).map(String::trim).filter(code -> !code.isEmpty()).forEach(result::add);
        return result;
    }

    private void validateAssignableCodes(Set<String> codes) {
        for (String code : codes) {
            if (!permissionCatalog.isAssignable(code)) {
                throw new BusinessException("存在不可分配或无效的 V3 权限编码: " + code);
            }
        }
    }

    private SysUserPermission buildOverride(String tenantCode, Long userId, Long permissionId, String effect) {
        SysUserPermission relation = new SysUserPermission();
        relation.setTenantCode(tenantCode);
        relation.setUserId(userId);
        relation.setPermissionId(permissionId);
        relation.setEffect(effect);
        relation.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        return relation;
    }

    private Map<String, Object> overrideSnapshot(Map<Long, String> effects,
                                                 Map<Long, SysPermission> permissionById) {
        Set<String> grants = effectCodes(effects, permissionById, SysUserPermission.EFFECT_GRANT);
        Set<String> denies = effectCodes(effects, permissionById, SysUserPermission.EFFECT_DENY);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("grants", grants);
        snapshot.put("denies", denies);
        return snapshot;
    }

    private void insertChangeLog(Long userId, Object before, Object after) {
        EmployeeChangeLog log = new EmployeeChangeLog();
        log.setTenantCode(TenantPermissionContext.getTenantCode());
        log.setEmployeeId(userId);
        log.setChangeType("PERMISSION_OVERRIDE");
        log.setBeforeJson(writeJson(before));
        log.setAfterJson(writeJson(after));
        log.setOperatorUserId(TenantPermissionContext.getUserId());
        employeeChangeLogMapper.insert(log);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("员工权限变更日志序列化失败");
        }
    }
}
