package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatTenantSelectRequest {
    @NotBlank(message = "请选择企业")
    private String tenantCode;
    @NotBlank(message = "企业选择凭证不能为空")
    private String selectionTicket;
}
