package my.hive.infrastructure.wechat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class WechatSubscribeRegisterRequest {

    @NotBlank
    private String code;

    @Valid
    @NotEmpty
    private List<TemplateSubscription> subscriptions;

    @Data
    public static class TemplateSubscription {
        @NotBlank
        private String templateId;
        @NotBlank
        private String status;
    }
}
