package my.hive.domain.auth.service;

import lombok.Data;

@Data
public class WebWechatIdentityPayload {
    private String subjectHash;
    private Long expireAt;
}
