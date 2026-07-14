package my.hive.domain.auth.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginRequest {
    @NotBlank
    private String phoneCode;
    private String tenantCode;
}
