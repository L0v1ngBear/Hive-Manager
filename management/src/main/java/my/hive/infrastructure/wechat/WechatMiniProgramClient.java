package my.hive.infrastructure.wechat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WechatMiniProgramClient {
    private final HttpClient http;
    private volatile String cachedToken;
    private volatile long tokenValidUntil;
    @Value("${wechat.mini-program.enabled:false}") boolean enabled;
    @Value("${wechat.mini-program.app-id:}") String appId;
    @Value("${wechat.mini-program.app-secret:}") String appSecret;

    public WechatMiniProgramClient() { this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()); }
    WechatMiniProgramClient(HttpClient http) { this.http = http; }

    public String getPhoneNumber(String code) {
        requireConfigured();
        JSONObject result = post("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken(), Map.of("code", code));
        if (result.getIntValue("errcode") != 0) throw failure("phone authorization");
        JSONObject info=result.getJSONObject("phone_info");
        String phone=info==null?null:info.getString("purePhoneNumber");
        if (blank(phone)&&info!=null) phone=info.getString("phoneNumber");
        if (blank(phone)) throw failure("phone authorization");
        return phone;
    }

    public String exchangeOpenId(String code) {
        requireConfigured();
        JSONObject result = get("https://api.weixin.qq.com/sns/jscode2session?appid=" + encode(appId)
                + "&secret=" + encode(appSecret) + "&js_code=" + encode(code)
                + "&grant_type=authorization_code");
        String openId = result.getString("openid");
        if (result.getIntValue("errcode") != 0 || blank(openId)) {
            throw failure("code exchange");
        }
        return openId;
    }

    public boolean sendSubscribeMessage(String openId,
                                        String templateId,
                                        String page,
                                        Map<String, String> fields) {
        requireConfigured();
        Map<String, Object> data = new LinkedHashMap<>();
        fields.forEach((key, value) -> data.put(key, Map.of("value", value)));
        JSONObject result = post("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken(),
                Map.of("touser", openId, "template_id", templateId,
                        "page", blank(page) ? "pages/index/index" : page, "data", data));
        return result.getIntValue("errcode") == 0;
    }

    private String accessToken() {
        long now=Instant.now().getEpochSecond();
        if (!blank(cachedToken)&&now<tokenValidUntil) return cachedToken;
        synchronized(this) {
            now=Instant.now().getEpochSecond();
            if (!blank(cachedToken)&&now<tokenValidUntil) return cachedToken;
            JSONObject result=get("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+encode(appId)+"&secret="+encode(appSecret));
            if (result.getIntValue("errcode")!=0 || blank(result.getString("access_token"))) throw failure("access token");
            cachedToken=result.getString("access_token");
            tokenValidUntil=now+Math.max(60,result.getLongValue("expires_in",7200)-300);
            return cachedToken;
        }
    }
    private JSONObject get(String url){return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build());}
    private JSONObject post(String url,Map<String,Object> body){return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body),StandardCharsets.UTF_8)).build());}
    private JSONObject send(HttpRequest request){
        try {
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()<200||response.statusCode()>=300) throw failure("request");
            JSONObject json=JSON.parseObject(response.body());
            if(json==null) throw failure("response");
            return json;
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw failure("request interrupted");}
        catch(BusinessException e){throw e;} catch(Exception e){throw failure("request");}
    }
    private void requireConfigured(){if(!enabled||blank(appId)||blank(appSecret))throw new BusinessException(503,"WeChat Mini Program login is not configured");}
    private BusinessException failure(String operation){return new BusinessException(502,"WeChat "+operation+" failed");}
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private boolean blank(String value){return value==null||value.isBlank();}
}
