package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebWechatCompleteRequest {
    @NotBlank(message = "微信登录凭证不能为空")
    private String loginTicket;
}
