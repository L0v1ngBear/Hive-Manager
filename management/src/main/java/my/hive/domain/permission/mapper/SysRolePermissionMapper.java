package my.hive.domain.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.permission.model.entity.SysRolePermission;
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

    @Insert("<script>"
            + "INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.roleId}, #{item.permissionId}, NOW(), 0)"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE is_deleted = 0"
            + "</script>")
    int upsertBatch(@Param("list") List<SysRolePermission> list);
}
