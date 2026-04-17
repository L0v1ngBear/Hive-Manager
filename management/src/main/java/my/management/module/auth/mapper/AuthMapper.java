package my.management.module.auth.mapper;

import my.management.module.auth.model.vo.LoginUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
/**
 * AuthMapper 属于管理端后端认证模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface AuthMapper {

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, u.name AS userName, u.login_name AS loginName, ",
            "u.phone AS phone, u.password AS password, u.status AS userStatus ",
            "FROM user u ",
            "WHERE (u.login_name = #{username} OR u.phone = #{username}) ",
            "LIMIT 1",
            "</script>"
    })
    LoginUserRow selectLoginUser(@Param("username") String username);

    @Select({
            "<script>",
            "SELECT DISTINCT p.perm_code ",
            "FROM sys_user_role ur ",
            "INNER JOIN sys_role r ON ur.role_id = r.id AND r.tenant_code = #{tenantCode} AND IFNULL(r.is_deleted, 0) = 0 ",
            "INNER JOIN sys_role_permission rp ON r.id = rp.role_id AND IFNULL(rp.is_deleted, 0) = 0 ",
            "INNER JOIN sys_permission p ON rp.permission_id = p.id AND IFNULL(p.is_deleted, 0) = 0 ",
            "WHERE ur.user_id = #{userId} AND IFNULL(ur.is_deleted, 0) = 0 ",
            "AND p.perm_code IS NOT NULL AND p.perm_code != ''",
            "</script>"
    })
    List<String> selectPermCodesByUserIdAndTenantCode(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);

    @Update("UPDATE user SET password = #{password} WHERE id = #{userId}")
    int updatePasswordByUserId(@Param("userId") Long userId, @Param("password") String password);

    @Select("SELECT login_name FROM user WHERE id = #{userId} AND tenant_code = #{tenantCode} LIMIT 1")
    String selectLoginNameByUserId(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);
}
