package my.hive.domain.employee.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.employee.model.entity.EmployeeExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
/**
 * EmployeeExtMapper 属于管理端后端员工模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface EmployeeExtMapper extends BaseMapper<EmployeeExt> {

    /** Includes logically deleted rows for the controlled rehire/owner-repair flow. */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id, user_id, tenant_code, emp_no, email, employee_type, entry_date,
                   avatar_url, remark, is_deleted, create_time, update_time
            FROM emp_employee_ext
            WHERE tenant_code = #{tenantCode}
              AND user_id = #{userId}
            LIMIT 1
            """)
    EmployeeExt selectIncludingDeleted(@Param("tenantCode") String tenantCode,
                                       @Param("userId") Long userId);

    /**
     * BaseMapper updates deliberately exclude logical-deleted rows.  Restoring
     * a previously deleted employee must therefore be an explicit, scoped
     * update rather than inserting a second extension row for the same user.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE emp_employee_ext
            SET emp_no = #{ext.empNo},
                email = #{ext.email},
                employee_type = #{ext.employeeType},
                entry_date = #{ext.entryDate},
                avatar_url = #{ext.avatarUrl},
                remark = #{ext.remark},
                is_deleted = 0,
                update_time = NOW()
            WHERE id = #{ext.id}
              AND user_id = #{ext.userId}
              AND tenant_code = #{ext.tenantCode}
            """)
    int restoreIncludingDeleted(@Param("ext") EmployeeExt ext);
}
