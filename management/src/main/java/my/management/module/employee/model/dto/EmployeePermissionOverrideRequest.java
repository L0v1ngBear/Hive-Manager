package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EmployeePermissionOverrideRequest {

    @NotNull(message = "员工ID不能为空")
    private Long userId;

    private List<Long> grantPermissionIds;

    private List<Long> denyPermissionIds;
}
