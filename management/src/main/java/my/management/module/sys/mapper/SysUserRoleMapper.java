package my.management.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.sys.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select({
            "<script>",
            "SELECT ur.role_id ",
            "FROM sys_user_role ur ",
            "WHERE ur.user_id = #{userId} ",
            "AND ur.tenant_code = #{tenantCode} ",
            "AND IFNULL(ur.is_deleted, 0) = 0",
            "</script>"
    })
    List<Long> selectRoleIdsByUserIdAndTenantCode(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);

    @Select({
            "<script>",
            "SELECT r.role_name ",
            "FROM sys_user_role ur ",
            "INNER JOIN sys_role r ON ur.role_id = r.id ",
            "WHERE ur.user_id = #{userId} ",
            "AND ur.tenant_code = #{tenantCode} ",
            "AND IFNULL(ur.is_deleted, 0) = 0 ",
            "AND IFNULL(r.is_deleted, 0) = 0 ",
            "ORDER BY r.id ASC",
            "</script>"
    })
    List<String> selectRoleNamesByUserIdAndTenantCode(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);
}
