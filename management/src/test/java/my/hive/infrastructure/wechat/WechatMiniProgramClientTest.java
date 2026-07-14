package my.hive.infrastructure.wechat;

import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WechatMiniProgramClientTest {
    @Test void cachesAccessTokenAcrossPhoneRequests() throws Exception {
        HttpClient http=mock(HttpClient.class); HttpResponse<String> token=response(200,"{\"access_token\":\"safe\",\"expires_in\":7200}"); HttpResponse<String> phone=response(200,"{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800000000\"}}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(token, phone, phone);
        WechatMiniProgramClient client=new WechatMiniProgramClient(http); configure(client);
        assertThat(client.getPhoneNumber("c1")).isEqualTo("13800000000"); assertThat(client.getPhoneNumber("c2")).isEqualTo("13800000000");
        verify(http,times(3)).send(any(), any(HttpResponse.BodyHandler.class));
    }
    @Test void rejectsNon2xxWithoutLeakingResponseOrSecret() throws Exception {
        HttpClient http=mock(HttpClient.class); HttpResponse<String> failed=response(500,"secret-body"); when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(failed);
        WechatMiniProgramClient client=new WechatMiniProgramClient(http); configure(client);
        assertThatThrownBy(() -> client.getPhoneNumber("code")).isInstanceOf(BusinessException.class).hasMessageNotContaining("secret");
    }
    @SuppressWarnings("unchecked") private HttpResponse<String> response(int status,String body){ HttpResponse<String> r=mock(HttpResponse.class); when(r.statusCode()).thenReturn(status); when(r.body()).thenReturn(body); return r; }
    private void configure(WechatMiniProgramClient c){ ReflectionTestUtils.setField(c,"enabled",true); ReflectionTestUtils.setField(c,"appId","app"); ReflectionTestUtils.setField(c,"appSecret","secret"); }
}
