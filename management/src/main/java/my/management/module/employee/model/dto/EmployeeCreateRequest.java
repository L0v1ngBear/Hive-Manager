package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "phone is required")
    private String phone;

    private String email;

    @NotBlank(message = "employeeType is required")
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

    private String avatarUrl;

    private String remark;

    private List<Long> roleIds;
}
