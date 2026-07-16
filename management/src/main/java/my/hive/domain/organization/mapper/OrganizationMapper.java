package my.hive.domain.organization.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.hive.domain.organization.model.vo.OrganizationEmployeeVO;
import my.hive.domain.organization.model.vo.OrganizationPositionVO;
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
@InterceptorIgnore(tenantLine = "true")
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
            "SELECT u.id, u.name, ext.emp_no AS empNo, COALESCE(u.phone_mask, u.phone) AS phone, u.department_name AS departmentName, ",
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

    @Select({
            "SELECT p.id, p.department_id AS departmentId, d.dept_name AS departmentName, ",
            "p.position_name AS positionName, p.position_code AS positionCode, p.sort_no AS sortNo, ",
            "p.status, p.create_time AS createTime, p.update_time AS updateTime, COUNT(u.id) AS employeeCount ",
            "FROM emp_position p ",
            "INNER JOIN emp_department d ON d.id = p.department_id AND d.tenant_code = p.tenant_code AND d.is_deleted = 0 ",
            "LEFT JOIN user u ON u.tenant_code = p.tenant_code AND u.department_name = d.dept_name AND u.position = p.position_name ",
            "WHERE p.tenant_code = #{tenantCode} AND p.department_id = #{departmentId} AND p.is_deleted = 0 ",
            "GROUP BY p.id, p.department_id, d.dept_name, p.position_name, p.position_code, p.sort_no, p.status, p.create_time, p.update_time ",
            "ORDER BY p.sort_no ASC, p.id ASC"
    })
    List<OrganizationPositionVO> selectPositions(@Param("tenantCode") String tenantCode,
                                                  @Param("departmentId") Long departmentId);

    @Select({
            "SELECT COUNT(1) FROM user ",
            "WHERE tenant_code = #{tenantCode} AND department_name = #{departmentName} AND position = #{positionName}"
    })
    Long countEmployeesByPosition(@Param("tenantCode") String tenantCode,
                                  @Param("departmentName") String departmentName,
                                  @Param("positionName") String positionName);

    @Update({
            "UPDATE user SET position = #{newName} ",
            "WHERE tenant_code = #{tenantCode} AND department_name = #{departmentName} AND position = #{oldName}"
    })
    int updateEmployeePositionName(@Param("tenantCode") String tenantCode,
                                   @Param("departmentName") String departmentName,
                                   @Param("oldName") String oldName,
                                   @Param("newName") String newName);
}
