package my.hive.domain.tenant.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantProfileUpdateRequest {

    @NotBlank(message = "企业名称不能为空")
    private String tenantName;

    private Integer tenantType;

    private String contactPerson;

    private String contactPhone;
}
