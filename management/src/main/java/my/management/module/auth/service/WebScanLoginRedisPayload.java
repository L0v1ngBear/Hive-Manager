package my.management.module.auth.service;

import my.management.module.auth.model.vo.LoginVO;
import lombok.Data;

/**
 * Redis 中保存的网页扫码登录会话载荷。
 */
@Data
public class WebScanLoginRedisPayload {

    private String status;

    private String message;

    private Long expireAt;

    private Long confirmedUserId;

    private String confirmedTenantCode;

    private String confirmedUserName;

    private LoginVO loginInfo;
}
