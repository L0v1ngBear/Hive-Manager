package my.management.module.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.model.entity.SysRole;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    public Page<SysRole> selectPage(Integer pages, Integer size, String keyword) {
        Page<SysRole> page = new Page<>(pages, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword);
        }
        return sysRoleMapper.selectPage(page, wrapper);
    }
}
