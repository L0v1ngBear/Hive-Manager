package my.hive.domain.permission.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
/**
 * SysRoleUpdateRequest 属于管理端后端系统模块，定义入参结构。
 */
@Data
public class SysRoleUpdateRequest {

    @NotNull
    private Long roleId;

    @NotNull
    private List<Long> permissionIds;
}
