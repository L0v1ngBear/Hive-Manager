package my.management.module.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.model.dto.SysRoleAddRequest;
import my.management.module.sys.model.dto.SysRoleUpdateRequest;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    public Page<SysRole> selectPage(Integer pages, Integer size, String keyword) {
        Page<SysRole> page = new Page<>(pages, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword);
        }
        return sysRoleMapper.selectPage(page, wrapper);
    }

    public List<SysPermission> selectAllPermission() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysPermission::getSort);
        return sysPermissionMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createNewRole(SysRoleAddRequest request) {
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setTenantCode(TenantPermissionContext.getTenantCode());

        // 1 非系统角色 0 系统角色
        role.setIsSystem(1);
        sysRoleMapper.insert(role);

        List<Long> permissionIds = request.getPermissionIds();

        // 2. 批量插入新权限
        List<SysRolePermission> list = permissionIds.stream()
                .map(pid -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(role.getId());
                    rp.setPermissionId(pid);
                    return rp;
                })
                .toList();

        // 3. 批量保存
        sysRolePermissionMapper.insertBatch(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(@Valid SysRoleUpdateRequest request) {
        // 1. 批量删除旧权限
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, request.getRoleId()));

        // 2. 批量插入新权限
        List<Long> permissionIds = request.getPermissionIds();
        sysRolePermissionMapper.insertBatch(permissionIds.stream()
                .map(pid -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(request.getRoleId());
                    rp.setPermissionId(pid);
                    return rp;
                })
                .toList());
    }
}
