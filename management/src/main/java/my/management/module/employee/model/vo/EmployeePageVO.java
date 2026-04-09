package my.management.module.employee.model.vo;

import lombok.Data;

import java.time.LocalDate;

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
    private String avatarUrl;
    private Long leaderId;
    private String leaderName;
    private String remark;
}
