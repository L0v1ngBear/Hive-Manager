package my.management.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.sys.model.entity.SysUserPermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserPermissionMapper extends BaseMapper<SysUserPermission> {

    @Insert("<script>"
            + "INSERT INTO sys_user_permission (tenant_code, user_id, permission_id, effect, create_time, update_time, is_deleted) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.tenantCode}, #{item.userId}, #{item.permissionId}, #{item.effect}, NOW(), NOW(), 0)"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE effect = VALUES(effect), update_time = NOW(), is_deleted = 0"
            + "</script>")
    int upsertBatch(@Param("list") List<SysUserPermission> list);
}
