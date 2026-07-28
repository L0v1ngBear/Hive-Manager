package my.hive.infrastructure.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatWebLoginClientTest {

    @Test
    void buildsOfficialWebsiteAuthorizationUrlWithoutExposingAppSecret() {
        WechatWebLoginProperties properties = new WechatWebLoginProperties();
        properties.setEnabled(true);
        properties.setAppId("wx-app-id");
        properties.setAppSecret("server-only-secret");
        properties.setCallbackUri("https://hive.example/api/auth/admin/wechat-login/callback");
        WechatWebLoginClient client = new WechatWebLoginClient(properties);

        String url = client.authorizationUrl("state-value");

        assertThat(url)
                .startsWith("https://open.weixin.qq.com/connect/qrconnect?")
                .contains("appid=wx-app-id")
                .contains("scope=snsapi_login")
                .contains("state=state-value")
                .contains("redirect_uri=https%3A%2F%2Fhive.example%2Fapi%2Fauth%2Fadmin%2Fwechat-login%2Fcallback")
                .doesNotContain("server-only-secret");
    }

    @Test
    void remainsDisabledUntilEveryServerSideSettingExists() {
        WechatWebLoginProperties properties = new WechatWebLoginProperties();
        properties.setEnabled(true);
        WechatWebLoginClient client = new WechatWebLoginClient(properties);

        assertThat(client.isEnabled()).isFalse();
        assertThatThrownBy(() -> client.authorizationUrl("state"))
                .hasMessageContaining("暂未配置");
    }
}
