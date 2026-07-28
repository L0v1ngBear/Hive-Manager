package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebWechatTenantSelectRequest {
    @NotBlank(message = "企业选择凭证不能为空")
    private String selectionTicket;
    @NotBlank(message = "企业不能为空")
    private String tenantCode;
}
