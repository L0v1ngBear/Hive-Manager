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
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE (u.login_name = #{username} ",
            "<if test='phoneHash != null and phoneHash != \"\"'>OR u.phone_hash = #{phoneHash} </if>",
            "OR u.phone = #{username}) ",
            "AND u.tenant_code = #{tenantCode} ",
            "LIMIT 1",
            "</script>"
    })
    LoginUserRow selectLoginUser(@Param("username") String username,
                                 @Param("phoneHash") String phoneHash,
                                 @Param("tenantCode") String tenantCode);

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE (u.login_name = #{username} ",
            "<if test='phoneHash != null and phoneHash != \"\"'>OR u.phone_hash = #{phoneHash} </if>",
            "OR u.phone = #{username}) ",
            "AND u.tenant_code IN ",
            "<foreach collection='tenantCodes' item='tenantCode' open='(' separator=',' close=')'>#{tenantCode}</foreach> ",
            "ORDER BY u.tenant_code ASC, u.id ASC ",
            "LIMIT 20",
            "</script>"
    })
    List<LoginUserRow> selectLoginUsers(@Param("username") String username,
                                        @Param("phoneHash") String phoneHash,
                                        @Param("tenantCodes") List<String> tenantCodes);

    @Select({
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE (u.phone_hash = #{phoneHash} OR u.phone = #{phone}) ",
            "LIMIT 1"
    })
    LoginUserRow selectLoginUserByPhone(@Param("phone") String phone, @Param("phoneHash") String phoneHash);

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE (u.phone_hash = #{phoneHash} OR u.phone = #{phone}) ",
            "AND u.tenant_code = #{tenantCode} ",
            "<if test='account != null and account != \"\"'>",
            "AND u.login_name = #{account} ",
            "</if>",
            "ORDER BY u.id ASC",
            "</script>"
    })
    List<LoginUserRow> selectLoginUsersByPhone(@Param("phone") String phone,
                                               @Param("phoneHash") String phoneHash,
                                               @Param("account") String account,
                                               @Param("tenantCode") String tenantCode);

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE (u.phone_hash = #{phoneHash} OR u.phone = #{phone}) ",
            "AND u.tenant_code IN ",
            "<foreach collection='tenantCodes' item='tenantCode' open='(' separator=',' close=')'>#{tenantCode}</foreach> ",
            "<if test='account != null and account != \"\"'>",
            "AND u.login_name = #{account} ",
            "</if>",
            "ORDER BY u.tenant_code ASC, u.id ASC",
            "</script>"
    })
    List<LoginUserRow> selectLoginUsersByPhoneInTenants(@Param("phone") String phone,
                                                        @Param("phoneHash") String phoneHash,
                                                        @Param("account") String account,
                                                        @Param("tenantCodes") List<String> tenantCodes);

    @Select({
            "SELECT u.id AS userId, u.tenant_code AS tenantCode, COALESCE(t.tenant_name, u.tenant_code) AS tenantName, t.logo_url AS tenantLogoUrl, u.name AS userName, u.login_name AS loginName, ",
            "COALESCE(u.phone_mask, u.phone) AS phone, u.password AS password, COALESCE(u.must_change_password, 0) AS mustChangePassword, u.status AS userStatus, COALESCE(u.permission_version, 1) AS permissionVersion, COALESCE(u.auth_version, 1) AS authVersion ",
            "FROM user u ",
            "LEFT JOIN tenant t ON t.tenant_code = u.tenant_code AND IFNULL(t.deleted, 0) = 0 ",
            "WHERE u.id = #{userId} AND u.tenant_code = #{tenantCode} ",
            "LIMIT 1"
    })
    LoginUserRow selectLoginUserByUserIdAndTenantCode(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);

    @Select({
            "<script>",
            "SELECT DISTINCT effective_perm.perm_code ",
            "FROM (",
            "  SELECT p.perm_code AS perm_code ",
            "  FROM sys_user_role ur ",
            "  INNER JOIN sys_role r ON ur.role_id = r.id AND r.tenant_code = #{tenantCode} AND IFNULL(r.is_deleted, 0) = 0 ",
            "  INNER JOIN sys_role_permission rp ON r.id = rp.role_id AND IFNULL(rp.is_deleted, 0) = 0 ",
            "  INNER JOIN sys_permission p ON rp.permission_id = p.id AND IFNULL(p.is_deleted, 0) = 0 AND p.status = 1 AND p.assignable = 1 ",
            "  WHERE ur.user_id = #{userId} AND ur.tenant_code = #{tenantCode} AND IFNULL(ur.is_deleted, 0) = 0 ",
            "  UNION ALL ",
            "  SELECT CASE WHEN up.effect = 'DENY' THEN CONCAT('!', p.perm_code) ELSE p.perm_code END AS perm_code ",
            "  FROM sys_user_permission up ",
            "  INNER JOIN sys_permission p ON p.id = up.permission_id AND IFNULL(p.is_deleted, 0) = 0 AND p.status = 1 AND p.assignable = 1 ",
            "  WHERE up.user_id = #{userId} AND up.tenant_code = #{tenantCode} AND IFNULL(up.is_deleted, 0) = 0 ",
            "  AND up.effect IN ('GRANT', 'DENY') ",
            ") effective_perm ",
            "WHERE effective_perm.perm_code IS NOT NULL AND effective_perm.perm_code != ''",
            "</script>"
    })
    List<String> selectPermCodesByUserIdAndTenantCode(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);

    @Update("UPDATE user SET password = #{password} WHERE id = #{userId} AND tenant_code = #{tenantCode}")
    int updatePasswordHashByUserIdAndTenantCode(@Param("userId") Long userId,
                                                @Param("tenantCode") String tenantCode,
                                                @Param("password") String password);

    @Update("UPDATE user SET password = #{password}, must_change_password = 0, auth_version = COALESCE(auth_version, 1) + 1 WHERE id = #{userId} AND tenant_code = #{tenantCode}")
    int updatePasswordByUserIdAndTenantCode(@Param("userId") Long userId,
                                            @Param("tenantCode") String tenantCode,
                                            @Param("password") String password);

    @Select("SELECT login_name FROM user WHERE id = #{userId} AND tenant_code = #{tenantCode} LIMIT 1")
    String selectLoginNameByUserId(@Param("userId") Long userId, @Param("tenantCode") String tenantCode);
}
