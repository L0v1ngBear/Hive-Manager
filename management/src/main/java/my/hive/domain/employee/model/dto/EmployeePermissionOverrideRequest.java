package my.hive.domain.employee.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Set;

@Data
public class EmployeePermissionOverrideRequest {

    @NotNull(message = "权限版本不能为空")
    @Positive(message = "权限版本不合法")
    private Long permissionVersion;

    private Set<@NotBlank(message = "授权编码不能为空") String> grants;

    private Set<@NotBlank(message = "禁用编码不能为空") String> denies;
}
