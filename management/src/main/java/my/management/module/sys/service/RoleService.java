package my.management.module.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.model.dto.SysRoleAddRequest;
import my.management.module.sys.model.dto.SysRoleUpdateRequest;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        wrapper.eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode());
        wrapper.orderByDesc(SysRole::getCreateTime);
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
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SysRole::getRoleName, request.getRoleName()));
        if (count > 0) {
            throw new BusinessException("role already exists");
        }

        SysRole role = new SysRole();
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
    }

    public Set<Long> getRolePermissionIds(Long roleId) {
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }
}