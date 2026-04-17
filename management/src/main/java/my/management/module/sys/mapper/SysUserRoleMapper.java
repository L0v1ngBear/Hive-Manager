package my.management.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.sys.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * SysUserRoleMapper 属于管理端后端系统模块，是数据访问类，负责与数据库交互。
 */
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

    @Select({
            "<script>",
            "SELECT ur.user_id AS userId, ur.role_id AS roleId ",
            "FROM sys_user_role ur ",
            "WHERE ur.tenant_code = #{tenantCode} ",
            "AND IFNULL(ur.is_deleted, 0) = 0 ",
            "AND ur.user_id IN ",
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>",
            "#{userId}",
            "</foreach>",
            "ORDER BY ur.user_id ASC, ur.role_id ASC",
            "</script>"
    })
    List<Map<String, Object>> selectRoleIdsByUserIds(@Param("tenantCode") String tenantCode,
                                                     @Param("userIds") Collection<Long> userIds);

    @Select({
            "<script>",
            "SELECT ur.user_id AS userId, r.role_name AS roleName ",
            "FROM sys_user_role ur ",
            "INNER JOIN sys_role r ON ur.role_id = r.id ",
            "WHERE ur.tenant_code = #{tenantCode} ",
            "AND IFNULL(ur.is_deleted, 0) = 0 ",
            "AND IFNULL(r.is_deleted, 0) = 0 ",
            "AND ur.user_id IN ",
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>",
            "#{userId}",
            "</foreach>",
            "ORDER BY ur.user_id ASC, r.id ASC",
            "</script>"
    })
    List<Map<String, Object>> selectRoleNamesByUserIds(@Param("tenantCode") String tenantCode,
                                                       @Param("userIds") Collection<Long> userIds);

    @Select({
            "SELECT DISTINCT user_id ",
            "FROM sys_user_role ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND role_id = #{roleId} ",
            "AND IFNULL(is_deleted, 0) = 0"
    })
    List<Long> selectUserIdsByRoleId(@Param("tenantCode") String tenantCode, @Param("roleId") Long roleId);
}
