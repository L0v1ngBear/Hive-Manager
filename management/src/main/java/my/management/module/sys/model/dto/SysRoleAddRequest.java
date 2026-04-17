package my.management.module.sys.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
/**
 * SysRoleAddRequest 属于管理端后端系统模块，定义入参结构。
 */
@Data
public class SysRoleAddRequest {

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 权限编码集合 (对应 SysRole.permCodes)
     */
    @NotEmpty(message = "关联权限不能为空")
    private List<Long> permissionIds;
}
