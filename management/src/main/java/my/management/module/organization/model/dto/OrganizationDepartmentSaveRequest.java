package my.management.module.organization.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组织部门保存入参，新增和编辑共用。
 */
@Data
public class OrganizationDepartmentSaveRequest {

    private Long id;

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private String deptCode;

    private String leaderName;

    private Integer sortNo;

    private Integer status;
}
