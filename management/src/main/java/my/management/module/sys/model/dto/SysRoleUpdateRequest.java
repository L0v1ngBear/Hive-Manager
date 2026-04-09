package my.management.module.sys.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SysRoleUpdateRequest {

    @NotNull
    private Long roleId;

    @NotNull
    private List<Long> permissionIds;
}
