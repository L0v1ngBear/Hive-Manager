package my.hive.domain.auth.model.vo;

import lombok.Data;

/**
 * 网页扫码登录会话信息。
 */
@Data
public class WebScanSessionVO {

    /**
     * 扫码场景键，前端轮询时会用到。
     */
    private String sceneKey;

    /**
     * 二维码图片的 Data URL。
     */
    private String qrCodeDataUrl;

    /**
     * 会话过期时间戳（秒）。
     */
    private Long expireAt;

    /**
     * 剩余有效秒数，方便前端显示倒计时。
     */
    private Long expiresInSeconds;
}
