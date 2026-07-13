package my.management.module.employee.model.dto;

import lombok.Data;

import java.time.LocalDate;
/**
 * EmployeePageQuery 属于管理端后端员工模块，定义入参结构。
 */
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
