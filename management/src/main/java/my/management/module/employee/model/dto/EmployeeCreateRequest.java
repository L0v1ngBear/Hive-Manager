package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
/**
 * EmployeeCreateRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "phone is required")
    private String phone;

    private String email;

    private String employeeType;

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @NotNull(message = "positionId is required")
    private Long positionId;

    private String leaderName;

    @NotNull(message = "entryDate is required")
    private LocalDate entryDate;

    @NotNull(message = "status is required")
    private Integer status;

    private Integer attendanceRequired = 1;

    private List<Long> attendanceLocationIds;

    private String remark;

    private List<Long> roleIds;
}
