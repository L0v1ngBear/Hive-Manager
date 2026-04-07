package my.management.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.sys.model.entity.SysRolePermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量插入角色权限关联关系
     * @param list 角色权限关联关系列表
     */
    @Insert("<script>"
            + "INSERT INTO sys_role_permission (role_id, permission_id) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.roleId}, #{item.permissionId})"
            + "</foreach>"
            + "</script>")
    int insertBatch(@Param("list") List<SysRolePermission> list);
}
