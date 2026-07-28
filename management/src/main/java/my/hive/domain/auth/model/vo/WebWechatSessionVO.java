package my.hive.domain.auth.model.vo;

import lombok.Data;

@Data
public class WebWechatSessionVO {
    private String authorizationUrl;
    private Long expiresInSeconds;
}
