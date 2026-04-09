package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeUpdateRequest extends EmployeeCreateRequest {

    @NotNull(message = "id is required")
    private Long id;
}
