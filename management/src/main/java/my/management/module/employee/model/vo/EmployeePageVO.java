package my.management.module.employee.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
/**
 * EmployeePageVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
public class EmployeePageVO {

    private Long id;
    private String name;
    private String empNo;
    private String employeeType;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String email;
    private String phone;
    private Integer status;
    private String statusLabel;
    private LocalDate entryDate;
    private Long leaderId;
    private String leaderName;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleNames;
}
