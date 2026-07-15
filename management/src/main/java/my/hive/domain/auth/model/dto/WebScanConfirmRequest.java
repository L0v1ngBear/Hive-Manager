package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 网页扫码登录确认请求。
 */
@Data
public class WebScanConfirmRequest {

    /**
     * 网页端创建的扫码场景键。
     */
    @NotBlank(message = "扫码场景不能为空")
    private String sceneKey;
}
