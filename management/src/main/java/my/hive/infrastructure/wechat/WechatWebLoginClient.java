package my.hive.infrastructure.wechat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WechatWebLoginClient {

    private static final String AUTHORIZE_ENDPOINT = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String TOKEN_ENDPOINT = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private final WechatWebLoginProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public WechatWebLoginClient(WechatWebLoginProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
    }

    WechatWebLoginClient(WechatWebLoginProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public boolean isEnabled() {
        return properties.isEnabled()
                && hasText(properties.getAppId())
                && hasText(properties.getAppSecret())
                && hasText(properties.getCallbackUri());
    }

    public String authorizationUrl(String state) {
        requireConfigured();
        return AUTHORIZE_ENDPOINT
                + "?appid=" + encode(properties.getAppId())
                + "&redirect_uri=" + encode(properties.getCallbackUri())
                + "&response_type=code&scope=snsapi_login"
                + "&state=" + encode(state)
                + "#wechat_redirect";
    }

    public WechatWebIdentity exchangeCode(String code) {
        requireConfigured();
        if (!hasText(code)) {
            throw new BusinessException(400, "微信授权凭证缺失，请重新扫码");
        }
        URI uri = URI.create(TOKEN_ENDPOINT
                + "?appid=" + encode(properties.getAppId())
                + "&secret=" + encode(properties.getAppSecret())
                + "&code=" + encode(code.trim())
                + "&grant_type=authorization_code");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw wechatFailure();
            }
            JSONObject body = JSON.parseObject(response.body());
            String openId = body == null ? null : body.getString("openid");
            if (body == null || body.getIntValue("errcode") != 0 || !hasText(openId)) {
                throw wechatFailure();
            }
            return new WechatWebIdentity(properties.getAppId().trim(), openId.trim(), clean(body.getString("unionid")));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw wechatFailure();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wechatFailure();
        }
    }

    public String frontendLoginPath() {
        String path = clean(properties.getFrontendLoginPath());
        return path != null && path.startsWith("/") && !path.startsWith("//") ? path : "/login";
    }

    private void requireConfigured() {
        if (!isEnabled()) {
            throw new BusinessException(503, "微信快捷登录暂未配置");
        }
    }

    private BusinessException wechatFailure() {
        return new BusinessException(502, "微信授权失败，请重新扫码或使用账号密码登录");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
