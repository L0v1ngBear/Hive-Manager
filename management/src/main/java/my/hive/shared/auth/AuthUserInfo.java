package my.hive.shared.auth;

import lombok.Data;
/**
 * AuthUserInfo 属于管理端后端通用能力层，提供认证或鉴权支撑逻辑。
 */
@Data
public class AuthUserInfo {

    private Long userId;

    private String tenantCode;

    private Long authVersion;

    private Long expireAt;
}
