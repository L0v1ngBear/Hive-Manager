package my.management.module.sys.service;

import my.hive.shared.permission.PermissionCatalogV3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.dto.SysRoleAddRequest;
import my.management.module.sys.model.dto.SysRoleUpdateRequest;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import my.management.module.sys.model.vo.SysPermissionTreeVO;
import my.management.module.employee.mapper.EmployeeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * RoleService 属于管理端后端系统模块，实现核心业务编排与规则逻辑。
 */
@Service
public class RoleService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private PermissionCatalogV3 permissionCatalog;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    @Resource
    private EmployeeMapper employeeMapper;

    public Page<SysRole> selectPage(Integer pages, Integer size, String keyword) {
        Page<SysRole> page = new Page<>(safePageNum(pages), safePageSize(size));
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode());
        wrapper.orderByDesc(SysRole::getCreateTime);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword);
        }
        return sysRoleMapper.selectPage(page, wrapper);
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

    public List<SysPermissionTreeVO> selectAllPermissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getParentId)
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId);
        List<SysPermission> permissionList = sysPermissionMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(permissionList)) {
            return List.of();
        }

        Map<Long, SysPermissionTreeVO> nodeMap = new LinkedHashMap<>();
        for (SysPermission permission : permissionList) {
            if (permission.getId() == null || nodeMap.containsKey(permission.getId())) {
                continue;
            }
            if (!Integer.valueOf(1).equals(permission.getStatus())
                    || !permissionCatalog.contains(permission.getPermCode())) {
                continue;
            }
            SysPermissionTreeVO node = new SysPermissionTreeVO();
            BeanUtils.copyProperties(permission, node);
            node.setValue(permission.getId());
            node.setLabel(permission.getPermName());
            node.setChildren(new ArrayList<>());
            nodeMap.put(node.getId(), node);
        }

        Map<Long, List<SysPermissionTreeVO>> childMap = new LinkedHashMap<>();
        List<SysPermissionTreeVO> rootNodes = new ArrayList<>();
        for (SysPermissionTreeVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                rootNodes.add(node);
                continue;
            }
            childMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
        }

        List<SysPermissionTreeVO> tree = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (SysPermissionTreeVO rootNode : rootNodes) {
            tree.add(fillChildren(rootNode, childMap, visited));
        }
        sortTree(tree);
        return tree;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createNewRole(SysRoleAddRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<Long> permissionIds = normalizePermissionIds(request.getPermissionIds());
        validateAssignablePermissionIds(permissionIds);

        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getRoleName, request.getRoleName())
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()));
        if (count > 0) {
            throw new BusinessException("role already exists");
        }

        SysRole role = new SysRole();
        role.setRoleCode(codeGeneratorUtil.generateRoleCode());
        role.setRoleName(request.getRoleName());
        role.setTenantCode(tenantCode);
        role.setIsSystem(BinaryFlagEnum.NO.getCode());
        sysRoleMapper.insert(role);

        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }

        List<SysRolePermission> list = permissionIds.stream()
                .map(pid -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(role.getId());
                    rp.setPermissionId(pid);
                    return rp;
                })
                .toList();
        sysRolePermissionMapper.upsertBatch(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(@Valid SysRoleUpdateRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        requireTenantRole(tenantCode, request.getRoleId());
        List<Long> permissionIds = normalizePermissionIds(request.getPermissionIds());
        validateAssignablePermissionIds(permissionIds);

        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, request.getRoleId()));

        if (CollectionUtils.isEmpty(permissionIds)) {
            evictRoleUsersPermissionCache(tenantCode, request.getRoleId());
            return;
        }

        sysRolePermissionMapper.upsertBatch(permissionIds.stream()
                .map(pid -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(request.getRoleId());
                    rp.setPermissionId(pid);
                    return rp;
                })
                .toList());
        evictRoleUsersPermissionCache(tenantCode, request.getRoleId());
    }

    public Set<Long> getRolePermissionIds(Long roleId) {
        requireTenantRole(TenantPermissionContext.getTenantCode(), roleId);
        Set<Long> permissionIds = sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
                        .eq(SysRolePermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode()))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return filterAssignablePermissionIds(permissionIds);
    }

    private void sortTree(List<SysPermissionTreeVO> nodes) {
        nodes.sort(Comparator
                .comparing(SysPermissionTreeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysPermissionTreeVO::getId, Comparator.nullsLast(Long::compareTo)));
        for (SysPermissionTreeVO node : nodes) {
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                sortTree(node.getChildren());
            }
        }
    }

    private SysPermissionTreeVO fillChildren(SysPermissionTreeVO node,
                                             Map<Long, List<SysPermissionTreeVO>> childMap,
                                             Set<Long> visited) {
        if (node.getId() == null || !visited.add(node.getId())) {
            node.setChildren(new ArrayList<>());
            return node;
        }

        List<SysPermissionTreeVO> childNodes = childMap.getOrDefault(node.getId(), Collections.emptyList());
        List<SysPermissionTreeVO> filledChildren = new ArrayList<>();
        for (SysPermissionTreeVO childNode : childNodes) {
            filledChildren.add(fillChildren(childNode, childMap, visited));
        }
        node.setChildren(filledChildren);
        return node;
    }

    private void evictRoleUsersPermissionCache(String tenantCode, Long roleId) {
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(tenantCode, roleId);
        for (Long userId : userIds) {
            if (employeeMapper.incrementPermissionVersion(tenantCode, userId) != 1) {
                throw new BusinessException(404, "员工不存在");
            }
            permissionCacheUtil.evict(tenantCode, userId);
        }
    }

    private Set<Long> filterAssignablePermissionIds(Set<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return new LinkedHashSet<>();
        }
        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getAssignable, 1));
        return permissions.stream()
                .filter(this::isAssignablePermission)
                .map(SysPermission::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private SysRole requireTenantRole(String tenantCode, Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new BusinessException("角色不存在或不属于当前组织");
        }
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getTenantCode, tenantCode)
                .eq(SysRole::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException("角色不存在或不属于当前组织");
        }
        return role;
    }

    private List<Long> normalizePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionIds.stream()
                .filter(Objects::nonNull)
                .filter(permissionId -> permissionId > 0)
                .distinct()
                .toList();
    }

    private void validateAssignablePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                .eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getAssignable, 1));
        Set<Long> validPermissionIds = new HashSet<>();
        for (SysPermission permission : permissions) {
            if (permission == null || permission.getId() == null) {
                continue;
            }
            if (!isAssignablePermission(permission)) {
                continue;
            }
            validPermissionIds.add(permission.getId());
        }
        if (validPermissionIds.size() != permissionIds.size()) {
            throw new BusinessException("存在不可分配或无效的权限");
        }
    }

    private boolean isAssignablePermission(SysPermission permission) {
        return permission != null
                && permission.getId() != null
                && Integer.valueOf(1).equals(permission.getStatus())
                && Integer.valueOf(1).equals(permission.getAssignable())
                && permissionCatalog.isAssignable(permission.getPermCode());
    }
}
