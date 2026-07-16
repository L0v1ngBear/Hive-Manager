package my.hive.domain.organization.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganizationPositionSaveRequest {

    private Long id;

    @NotNull(message = "部门不能为空")
    private Long departmentId;

    @NotBlank(message = "职位名称不能为空")
    private String positionName;

    private String positionCode;

    private Integer sortNo;

    private Integer status;
}
