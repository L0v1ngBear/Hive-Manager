package my.hive.domain.auth.service;

import lombok.Data;

@Data
public class WechatPhoneVerificationPayload {
    private String phoneHash;
    private Long expireAt;
}
