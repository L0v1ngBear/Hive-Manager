package my.management.module.organization.mapper;

import my.management.module.organization.model.vo.OrganizationEmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 组织架构数据访问层，负责部门维度的员工统计和部门名称同步。
 */
@Mapper
public interface OrganizationMapper {

    @Select({
            "SELECT department_name AS departmentName, COUNT(1) AS employeeCount ",
            "FROM user ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND department_name IS NOT NULL AND department_name <> '' ",
            "GROUP BY department_name"
    })
    List<Map<String, Object>> selectDepartmentEmployeeCounts(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT department_id AS departmentId, COUNT(1) AS positionCount ",
            "FROM emp_position ",
            "WHERE tenant_code = #{tenantCode} AND is_deleted = 0 ",
            "GROUP BY department_id"
    })
    List<Map<String, Object>> selectDepartmentPositionCounts(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT u.id, u.name, ext.emp_no AS empNo, u.phone, u.department_name AS departmentName, ",
            "u.position AS positionName, u.status ",
            "FROM user u ",
            "LEFT JOIN emp_employee_ext ext ON ext.user_id = u.id AND ext.tenant_code = u.tenant_code AND ext.is_deleted = 0 ",
            "WHERE u.tenant_code = #{tenantCode} ",
            "AND u.department_name = #{departmentName} ",
            "ORDER BY u.status DESC, u.id DESC"
    })
    List<OrganizationEmployeeVO> selectEmployeesByDepartment(@Param("tenantCode") String tenantCode,
                                                             @Param("departmentName") String departmentName);

    @Update({
            "UPDATE user ",
            "SET department_name = #{newName} ",
            "WHERE tenant_code = #{tenantCode} AND department_name = #{oldName}"
    })
    int updateEmployeeDepartmentName(@Param("tenantCode") String tenantCode,
                                     @Param("oldName") String oldName,
                                     @Param("newName") String newName);
}
