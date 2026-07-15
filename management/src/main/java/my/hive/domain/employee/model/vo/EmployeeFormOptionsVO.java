package my.hive.domain.employee.model.vo;

import lombok.Data;

import java.util.List;
/**
 * EmployeeFormOptionsVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
public class EmployeeFormOptionsVO {

    private List<DepartmentOptionVO> departments;
    private List<PositionOptionVO> positions;
    private List<RoleOptionVO> roles;
    private List<OptionVO> attendanceLocations;
    private List<OptionVO> employeeTypes;
    private List<OptionVO> employmentStatuses;
}
