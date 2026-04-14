package my.management.module.employee.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeFormOptionsVO {

    private List<DepartmentOptionVO> departments;
    private List<PositionOptionVO> positions;
    private List<RoleOptionVO> roles;
    private List<OptionVO> employeeTypes;
    private List<OptionVO> employmentStatuses;
}
