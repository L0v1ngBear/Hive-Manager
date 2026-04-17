package my.management.module.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * RoleService 属于管理端后端系统模块，实现核心业务编排与规则逻辑。
 */
@Service
public class RoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    public Page<SysRole> selectPage(Integer pages, Integer size, String keyword) {
        Page<SysRole> page = new Page<>(pages, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode());
        wrapper.orderByDesc(SysRole::getCreateTime);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword);
        }
        return sysRoleMapper.selectPage(page, wrapper);
    }

    public List<SysPermissionTreeVO> selectAllPermissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysPermission::getParentId)
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
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SysRole::getRoleName, request.getRoleName()));
        if (count > 0) {
            throw new BusinessException("role already exists");
        }

        SysRole role = new SysRole();
        role.setRoleCode(codeGeneratorUtil.generateRoleCode());
        role.setRoleName(request.getRoleName());
        role.setTenantCode(TenantPermissionContext.getTenantCode());
        role.setIsSystem(0);
        sysRoleMapper.insert(role);

        if (CollectionUtils.isEmpty(request.getPermissionIds())) {
            return;
        }

        List<SysRolePermission> list = request.getPermissionIds().stream()
                .map(pid -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(role.getId());
                    rp.setPermissionId(pid);
                    return rp;
                })
                .toList();
        sysRolePermissionMapper.insertBatch(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(@Valid SysRoleUpdateRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, request.getRoleId()));

        if (CollectionUtils.isEmpty(request.getPermissionIds())) {
            return;
        }

        sysRolePermissionMapper.insertBatch(request.getPermissionIds().stream()
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
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toSet());
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
        userIds.forEach(userId -> permissionCacheUtil.evict(tenantCode, userId));
    }
}
