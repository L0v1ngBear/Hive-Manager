package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeStatusChangeRequest {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "status is required")
    private Integer status;

    private String remark;
}
