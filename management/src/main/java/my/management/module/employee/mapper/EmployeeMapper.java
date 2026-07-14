package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import my.hive.domain.approval.model.vo.ApprovalAuditorOptionVO;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.vo.EmployeeDetailVO;
import my.management.module.employee.model.vo.EmployeeLeaderOptionVO;
import my.management.module.employee.model.vo.EmployeePageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
/**
 * EmployeeMapper 属于管理端后端员工模块，是数据访问类，负责与数据库交互。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface EmployeeMapper extends BaseMapper<Employee> {

    @Update("""
            UPDATE `user`
            SET permission_version = COALESCE(permission_version, 1) + 1,
                update_time = NOW()
            WHERE tenant_code = #{tenantCode}
              AND id = #{userId}
              AND permission_version = #{expectedVersion}
            """)
    int incrementPermissionVersionIfCurrent(@Param("tenantCode") String tenantCode,
                                            @Param("userId") Long userId,
                                            @Param("expectedVersion") Long expectedVersion);

    @Update("""
            UPDATE `user`
            SET permission_version = COALESCE(permission_version, 1) + 1,
                update_time = NOW()
            WHERE tenant_code = #{tenantCode}
              AND id = #{userId}
            """)
    int incrementPermissionVersion(@Param("tenantCode") String tenantCode,
                                   @Param("userId") Long userId);

    @Update("""
            UPDATE `user`
            SET auth_version = COALESCE(auth_version, 1) + 1,
                update_time = NOW()
            WHERE tenant_code = #{tenantCode}
              AND id = #{userId}
            """)
    int incrementAuthVersion(@Param("tenantCode") String tenantCode,
                             @Param("userId") Long userId);

    @Update("""
            UPDATE `user`
            SET permission_version = COALESCE(permission_version, 1) + 1,
                auth_version = COALESCE(auth_version, 1) + 1,
                update_time = NOW()
            WHERE tenant_code = #{tenantCode}
              AND id = #{userId}
            """)
    int incrementPermissionAndAuthVersion(@Param("tenantCode") String tenantCode,
                                          @Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT u.id, u.name, ext.emp_no AS empNo, ext.employee_type AS employeeType, ",
            "d.id AS departmentId, u.department_name AS departmentName, p.id AS positionId, u.position AS positionName, ",
            "ext.email AS email, COALESCE(u.phone_mask, u.phone) AS phone, u.status AS status, COALESCE(u.attendance_required, 1) AS attendanceRequired, ext.entry_date AS entryDate, ",
            "u.manager_id AS leaderId, u.manager_name AS leaderName, ext.remark AS remark ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "LEFT JOIN emp_department d ON d.dept_name = u.department_name AND d.tenant_code = u.tenant_code AND d.is_deleted = 0 ",
            "LEFT JOIN emp_position p ON p.position_name = u.position AND p.tenant_code = u.tenant_code AND p.is_deleted = 0 ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (u.name LIKE CONCAT('%', #{keyword}, '%') ",
            "       OR COALESCE(u.phone_mask, u.phone) LIKE CONCAT('%', #{keyword}, '%') ",
            "       OR ext.emp_no LIKE CONCAT('%', #{keyword}, '%') ",
            "       <if test='keywordPhoneHash != null and keywordPhoneHash != \"\"'>OR u.phone_hash = #{keywordPhoneHash} </if>) ",
            "</if>",
            "<if test='departmentId != null'>AND d.id = #{departmentId} </if>",
            "<if test='status != null'>AND u.status = #{status} </if>",
            "<if test='employeeType != null and employeeType != \"\"'>AND ext.employee_type = #{employeeType} </if>",
            "<if test='entryDateStart != null'>AND ext.entry_date &gt;= #{entryDateStart} </if>",
            "<if test='entryDateEnd != null'>AND ext.entry_date &lt;= #{entryDateEnd} </if>",
            "ORDER BY u.id DESC",
            "</script>"
    })
    Page<EmployeePageVO> selectEmployeePage(Page<EmployeePageVO> page,
                                            @Param("tenantCode") String tenantCode,
                                            @Param("keyword") String keyword,
                                            @Param("keywordPhoneHash") String keywordPhoneHash,
                                            @Param("departmentId") Long departmentId,
                                            @Param("status") Integer status,
                                            @Param("employeeType") String employeeType,
                                            @Param("entryDateStart") LocalDate entryDateStart,
                                            @Param("entryDateEnd") LocalDate entryDateEnd);

    @Select({
            "SELECT u.id, u.name, ext.emp_no AS empNo, ext.employee_type AS employeeType, ",
            "d.id AS departmentId, u.department_name AS departmentName, p.id AS positionId, u.position AS positionName, ",
            "ext.email AS email, COALESCE(u.phone_mask, u.phone) AS phone, u.status AS status, COALESCE(u.attendance_required, 1) AS attendanceRequired, ext.entry_date AS entryDate, ",
            "u.manager_id AS leaderId, u.manager_name AS leaderName, ext.remark AS remark ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "LEFT JOIN emp_department d ON d.dept_name = u.department_name AND d.tenant_code = u.tenant_code AND d.is_deleted = 0 ",
            "LEFT JOIN emp_position p ON p.position_name = u.position AND p.tenant_code = u.tenant_code AND p.is_deleted = 0 ",
            "WHERE u.tenant_code = #{tenantCode} AND u.id = #{id} LIMIT 1"
    })
    EmployeeDetailVO selectEmployeeDetail(@Param("tenantCode") String tenantCode, @Param("id") Long id);

    @Select({
            "<script>",
            "SELECT u.id, u.name, ext.emp_no AS empNo, ext.employee_type AS employeeType, ",
            "d.id AS departmentId, u.department_name AS departmentName, p.id AS positionId, u.position AS positionName, ",
            "ext.email AS email, COALESCE(u.phone_mask, u.phone) AS phone, u.status AS status, COALESCE(u.attendance_required, 1) AS attendanceRequired, ext.entry_date AS entryDate, ",
            "u.manager_id AS leaderId, u.manager_name AS leaderName, ext.remark AS remark ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "LEFT JOIN emp_department d ON d.dept_name = u.department_name AND d.tenant_code = u.tenant_code AND d.is_deleted = 0 ",
            "LEFT JOIN emp_position p ON p.position_name = u.position AND p.tenant_code = u.tenant_code AND p.is_deleted = 0 ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "AND (ext.id IS NULL OR ext.is_deleted = 0) ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR COALESCE(u.phone_mask, u.phone) LIKE CONCAT('%', #{keyword}, '%') OR ext.emp_no LIKE CONCAT('%', #{keyword}, '%') ",
            "<if test='keywordPhoneHash != null and keywordPhoneHash != \"\"'>OR u.phone_hash = #{keywordPhoneHash} </if>) ",
            "</if>",
            "<if test='departmentId != null'>AND d.id = #{departmentId} </if>",
            "<if test='status != null'>AND u.status = #{status} </if>",
            "<if test='employeeType != null and employeeType != \"\"'>AND ext.employee_type = #{employeeType} </if>",
            "<if test='entryDateStart != null'>AND ext.entry_date <![CDATA[ >= ]]> #{entryDateStart} </if>",
            "<if test='entryDateEnd != null'>AND ext.entry_date <![CDATA[ <= ]]> #{entryDateEnd} </if>",
            "ORDER BY u.id DESC LIMIT #{limit}",
            "</script>"
    })
    List<EmployeePageVO> selectEmployeeExport(@Param("tenantCode") String tenantCode,
                                              @Param("keyword") String keyword,
                                              @Param("keywordPhoneHash") String keywordPhoneHash,
                                              @Param("departmentId") Long departmentId,
                                              @Param("status") Integer status,
                                              @Param("employeeType") String employeeType,
                                              @Param("entryDateStart") LocalDate entryDateStart,
                                              @Param("entryDateEnd") LocalDate entryDateEnd,
                                              @Param("limit") Integer limit);

    @Select({
            "<script>",
            "SELECT u.id, u.name, ext.emp_no AS empNo, u.department_name AS departmentName, u.position AS positionName ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "AND (ext.id IS NULL OR ext.is_deleted = 0) ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR ext.emp_no LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "ORDER BY u.id DESC LIMIT #{limit}",
            "</script>"
    })
    List<EmployeeLeaderOptionVO> searchLeaders(@Param("tenantCode") String tenantCode,
                                               @Param("keyword") String keyword,
                                               @Param("limit") Integer limit);

    @Select("SELECT COUNT(1) FROM user u LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code WHERE u.tenant_code = #{tenantCode} AND (ext.id IS NULL OR ext.is_deleted = 0)")
    Long countAvailableEmployees(@Param("tenantCode") String tenantCode);

    @Select("SELECT COUNT(DISTINCT department_name) FROM user WHERE tenant_code = #{tenantCode} AND department_name IS NOT NULL AND department_name != ''")
    Long countDistinctDepartments(@Param("tenantCode") String tenantCode);

    @Select("SELECT COUNT(1) FROM user u JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 WHERE u.tenant_code = #{tenantCode} AND ext.entry_date > CURRENT_DATE() AND (u.status IS NULL OR u.status != 1)")
    Long countPendingOnboard(@Param("tenantCode") String tenantCode);

    @Select("""
            SELECT COUNT(DISTINCT a.user_id)
            FROM attendance_record a
            INNER JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code AND COALESCE(u.attendance_required, 1) = 1
            WHERE a.tenant_code = #{tenantCode}
              AND a.punch_id LIKE CONCAT(DATE_FORMAT(CURRENT_DATE(), '%Y%m%d'), '%')
              AND a.sign_in_status IS NOT NULL
            """)
    Long countTodayAttendanceUsers(@Param("tenantCode") String tenantCode);

    @Select("""
            SELECT u.id
            FROM user u
            WHERE u.tenant_code = #{tenantCode}
              AND IFNULL(u.status, 1) <> 0
              AND (
                EXISTS (
                  SELECT 1
                  FROM sys_user_role ur
                  INNER JOIN sys_role r
                    ON r.id = ur.role_id AND r.tenant_code = u.tenant_code AND IFNULL(r.is_deleted, 0) = 0
                  INNER JOIN sys_role_permission rp
                    ON rp.role_id = r.id AND IFNULL(rp.is_deleted, 0) = 0
                  INNER JOIN sys_permission p
                    ON p.id = rp.permission_id AND IFNULL(p.is_deleted, 0) = 0
                   AND p.status = 1 AND p.assignable = 1
                  WHERE ur.user_id = u.id
                    AND ur.tenant_code = u.tenant_code
                    AND IFNULL(ur.is_deleted, 0) = 0
                    AND p.perm_code = #{permissionCode}
                )
                OR EXISTS (
                  SELECT 1
                  FROM sys_user_permission up
                  INNER JOIN sys_permission p
                    ON p.id = up.permission_id AND IFNULL(p.is_deleted, 0) = 0
                   AND p.status = 1 AND p.assignable = 1
                  WHERE up.user_id = u.id
                    AND up.tenant_code = u.tenant_code
                    AND IFNULL(up.is_deleted, 0) = 0
                    AND up.effect = 'GRANT'
                    AND p.perm_code = #{permissionCode}
                )
              )
              AND NOT EXISTS (
                SELECT 1
                FROM sys_user_permission up
                INNER JOIN sys_permission p
                  ON p.id = up.permission_id AND IFNULL(p.is_deleted, 0) = 0
                 AND p.status = 1 AND p.assignable = 1
                WHERE up.user_id = u.id
                  AND up.tenant_code = u.tenant_code
                  AND IFNULL(up.is_deleted, 0) = 0
                  AND up.effect = 'DENY'
                  AND p.perm_code = #{permissionCode}
              )
            GROUP BY u.id
            ORDER BY MAX(COALESCE(u.role_level, 0)) DESC, u.id ASC
            """)
    List<Long> selectActiveApproverIdsByPermission(@Param("tenantCode") String tenantCode,
                                                   @Param("permissionCode") String permissionCode);

    @Select({
            "<script>",
            "SELECT u.id, u.name, ext.emp_no AS empNo, ",
            "u.department_name AS departmentName, u.position AS positionName ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ",
            "  ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND IFNULL(ext.is_deleted, 0) = 0 ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "AND IFNULL(u.status, 1) &lt;&gt; 0 ",
            "AND (EXISTS (SELECT 1 FROM sys_user_role ur ",
            "  INNER JOIN sys_role r ON r.id = ur.role_id AND r.tenant_code = u.tenant_code AND IFNULL(r.is_deleted, 0) = 0 ",
            "  INNER JOIN sys_role_permission rp ON rp.role_id = r.id AND IFNULL(rp.is_deleted, 0) = 0 ",
            "  INNER JOIN sys_permission p ON p.id = rp.permission_id AND IFNULL(p.is_deleted, 0) = 0 AND p.status = 1 AND p.assignable = 1 ",
            "  WHERE ur.user_id = u.id AND ur.tenant_code = u.tenant_code AND IFNULL(ur.is_deleted, 0) = 0 ",
            "  AND p.perm_code = #{permissionCode}) ",
            "OR EXISTS (SELECT 1 FROM sys_user_permission up ",
            "  INNER JOIN sys_permission p ON p.id = up.permission_id AND IFNULL(p.is_deleted, 0) = 0 AND p.status = 1 AND p.assignable = 1 ",
            "  WHERE up.user_id = u.id AND up.tenant_code = u.tenant_code AND IFNULL(up.is_deleted, 0) = 0 ",
            "  AND up.effect = 'GRANT' ",
            "  AND p.perm_code = #{permissionCode})) ",
            "AND NOT EXISTS (SELECT 1 FROM sys_user_permission up ",
            "  INNER JOIN sys_permission p ON p.id = up.permission_id AND IFNULL(p.is_deleted, 0) = 0 AND p.status = 1 AND p.assignable = 1 ",
            "  WHERE up.user_id = u.id AND up.tenant_code = u.tenant_code AND IFNULL(up.is_deleted, 0) = 0 ",
            "  AND up.effect = 'DENY' ",
            "  AND p.perm_code = #{permissionCode}) ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') ",
            "     OR ext.emp_no LIKE CONCAT('%', #{keyword}, '%') ",
            "     OR u.department_name LIKE CONCAT('%', #{keyword}, '%') ",
            "     OR u.position LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "GROUP BY u.id, u.name, ext.emp_no, u.department_name, u.position ",
            "ORDER BY MAX(COALESCE(u.role_level, 0)) DESC, u.id ASC ",
            "LIMIT #{limit}",
            "</script>"
    })
    List<ApprovalAuditorOptionVO> selectActiveApproverOptionsByPermission(@Param("tenantCode") String tenantCode,
                                                                          @Param("permissionCode") String permissionCode,
                                                                          @Param("keyword") String keyword,
                                                                          @Param("limit") Integer limit);
}
