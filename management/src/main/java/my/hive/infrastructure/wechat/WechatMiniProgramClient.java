package my.hive.infrastructure.wechat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Adapter for WeChat Mini Program phone-number authorization. */
@Component
public class WechatMiniProgramClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    @Value("${wechat.mini-program.enabled:false}") boolean enabled;
    @Value("${wechat.mini-program.app-id:}") String appId;
    @Value("${wechat.mini-program.app-secret:}") String appSecret;

    public String getPhoneNumber(String phoneCode) {
        if (!enabled || blank(appId) || blank(appSecret)) throw new BusinessException(503, "WeChat Mini Program login is not configured");
        String token = accessToken();
        JSONObject response = post("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + token,
                Map.of("code", phoneCode));
        if (response.getIntValue("errcode") != 0) throw new BusinessException(502, "WeChat phone authorization failed");
        JSONObject info = response.getJSONObject("phone_info");
        String phone = info == null ? null : info.getString("purePhoneNumber");
        if (blank(phone) && info != null) phone = info.getString("phoneNumber");
        if (blank(phone)) throw new BusinessException(502, "WeChat did not return a phone number");
        return phone;
    }

    private String accessToken() {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        JSONObject response = get(url);
        String token = response.getString("access_token");
        if (blank(token)) throw new BusinessException(502, "WeChat access-token request failed");
        return token;
    }
    private JSONObject get(String url) { return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build()); }
    private JSONObject post(String url, Map<String,Object> body) { return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8)).build()); }
    private JSONObject send(HttpRequest request) {
        try { return JSON.parseObject(http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body()); }
        catch (Exception e) { Thread.currentThread().interrupt(); throw new BusinessException(502, "WeChat request failed"); }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
