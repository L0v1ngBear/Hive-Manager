package my.management.module.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetCodeRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Size(max = 64, message = "登录账号长度不能超过64位")
    private String account;
}
