package my.management.module.employee.model.vo;

import lombok.Data;
/**
 * EmployeeLeaderOptionVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
public class EmployeeLeaderOptionVO {

    private Long id;
    private String name;
    private String empNo;
    private String departmentName;
    private String positionName;
}
