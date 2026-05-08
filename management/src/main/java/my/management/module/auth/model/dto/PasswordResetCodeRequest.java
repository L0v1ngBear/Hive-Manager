package my.management.module.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetCodeRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;
}
