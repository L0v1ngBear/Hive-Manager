package my.management.module.employee.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeePageQuery {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private Long departmentId;

    private Integer status;

    private String employeeType;

    private LocalDate entryDateStart;

    private LocalDate entryDateEnd;
}
