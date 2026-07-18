package my.hive.domain.employee.model.vo;

import lombok.Data;

@Data
public class EmployeeCreateVO {

    private Long employeeId;
    private String empNo;
    private String phoneMask;
    private Boolean activationRequired;
}
