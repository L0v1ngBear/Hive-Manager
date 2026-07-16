package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Kuaidi100ClientTest {

    @Test
    void signsAndNormalizesOfficialRealtimeQueryResponse() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = response(200, """
                {"message":"ok","nu":"SF123456","ischeck":"1","com":"shunfeng","status":"200","state":"3","data":[
                  {"context":"已签收","time":"2026-07-16 12:30:00","ftime":"2026-07-16 12:30:00","status":"签收","statusCode":"501","location":"上海市"}
                ]}
                """);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        Kuaidi100Properties properties = configuredProperties();
        Kuaidi100Client client = new Kuaidi100Client(properties, http);

        OrderLogisticsTrackingVO result = client.query("shunfeng", "SF123456", "13800000000");

        assertThat(result.getCompanyCode()).isEqualTo("shunfeng");
        assertThat(result.getExpressNo()).isEqualTo("SF123456");
        assertThat(result.getState()).isEqualTo("3");
        assertThat(result.getStateLabel()).isEqualTo("已签收");
        assertThat(result.getLatestContext()).isEqualTo("已签收");
        assertThat(result.getTraces()).hasSize(1);
        assertThat(result.getTraces().get(0).getStatusCode()).isEqualTo("501");

        String param = Kuaidi100Client.buildParam("shunfeng", "SF123456", "13800000000");
        assertThat(client.sign(param)).isEqualTo("8BA0273CD72D5A8195F3316AC28ABB00");
        assertThat(Kuaidi100Client.formBody(param, client.sign(param), properties.getCustomer()))
                .contains("customer=customer-code", "sign=", "param=");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(requestCaptor.getValue().uri().toString())
                .isEqualTo("https://poll.kuaidi100.com/poll/query.do");
    }

    @Test
    void rejectsMissingConfigurationAndMapsPhoneValidationWithoutLeakingProviderBody() throws Exception {
        Kuaidi100Properties disabled = new Kuaidi100Properties();
        assertThatThrownBy(() -> new Kuaidi100Client(disabled, mock(HttpClient.class))
                .query("shunfeng", "SF123456", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未配置");

        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> phoneValidation = response(
                200,
                "{\"status\":\"408\",\"message\":\"private-provider-body\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(phoneValidation);

        assertThatThrownBy(() -> new Kuaidi100Client(configuredProperties(), http)
                .query("shunfeng", "SF123456", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号")
                .hasMessageNotContaining("private-provider-body");
    }

    private Kuaidi100Properties configuredProperties() {
        Kuaidi100Properties properties = new Kuaidi100Properties();
        properties.setEnabled(true);
        properties.setKey("secret-key");
        properties.setCustomer("customer-code");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
