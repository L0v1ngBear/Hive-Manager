package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebWechatBindRequest {
    @NotBlank(message = "微信绑定凭证不能为空")
    private String bindingTicket;
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
