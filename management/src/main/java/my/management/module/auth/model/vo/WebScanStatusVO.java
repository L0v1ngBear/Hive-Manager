package my.management.module.auth.model.vo;

import lombok.Data;

/**
 * 网页扫码登录状态。
 */
@Data
public class WebScanStatusVO {

    /**
     * PENDING-待扫码，CONFIRMED-已确认，EXPIRED-已过期。
     */
    private String status;

    /**
     * 最近一次状态说明，给前端直接展示。
     */
    private String message;

    /**
     * 会话过期时间戳（秒）。
     */
    private Long expireAt;

    /**
     * 扫码确认成功后返回网页端真实登录信息。
     */
    private LoginVO loginInfo;
}
