package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * EmployeeUpdateRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeUpdateRequest extends EmployeeCreateRequest {

    @NotNull(message = "id is required")
    private Long id;
}
