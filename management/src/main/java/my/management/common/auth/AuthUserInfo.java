package my.management.common.auth;

import lombok.Data;

@Data
public class AuthUserInfo {

    private Long userId;

    private String tenantCode;

    private Long expireAt;
}