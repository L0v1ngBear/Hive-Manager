package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliyunMarketLogisticsTrackingProviderTest {

    @Test
    void sendsTheDocumentedGetParametersAndNormalizesTheDataWrapper() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, successBody());
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(apiResponse);
        AliyunMarketLogisticsTrackingProvider provider = new AliyunMarketLogisticsTrackingProvider(configuredProperties(), http);

        OrderLogisticsTrackingVO result = provider.query(new LogisticsTrackingQuery("DBKD", "DPK212715395868", "0000"));

        assertThat(provider.providerCode()).isEqualTo("aliyun-market");
        assertThat(provider.supportsCompanyCodeAutoRecognition()).isTrue();
        assertThat(result.getCompany()).isEqualTo("德邦快递");
        assertThat(result.getCompanyCode()).isEqualTo("DBKD");
        assertThat(result.getTrackingNo()).isEqualTo("DPK212715395868");
        assertThat(result.getStateLabel()).isEqualTo("已签收");
        assertThat(result.getLatestContext()).isEqualTo("正常签收");
        assertThat(result.getTraces()).extracting(OrderLogisticsTrackingVO.TraceVO::getContext)
                .containsExactly("已签收", "运输中");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.uri().toString()).isEqualTo(
                "https://kzexpress.market.alicloudapi.com/api-mall/api/express/query"
                        + "?expressNo=DPK212715395868&mobile=0000&cpCode=DBKD");
        assertThat(request.headers().firstValue("Authorization")).contains("APPCODE test-app-code");
    }

    @Test
    void omitsOptionalMobileAndCompanyCodeForAutoRecognition() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, successBody());
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(apiResponse);

        new AliyunMarketLogisticsTrackingProvider(configuredProperties(), http)
                .query(new LogisticsTrackingQuery(null, "SF 123/456", null));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo(
                "https://kzexpress.market.alicloudapi.com/api-mall/api/express/query?expressNo=SF+123%2F456");
    }

    @Test
    void rejectsBlankConfigurationAndProviderFailuresWithoutLeakingResponseData() throws Exception {
        AliyunMarketLogisticsProperties disabled = new AliyunMarketLogisticsProperties();
        assertThatThrownBy(() -> new AliyunMarketLogisticsTrackingProvider(disabled, mock(HttpClient.class))
                .query(new LogisticsTrackingQuery("DBKD", "DPK212715395868", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getMessage())
                .isEqualTo("阿里云物流查询未配置");

        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, "{\"success\":false,\"msg\":\"raw-provider-message\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(apiResponse);
        assertThatThrownBy(() -> new AliyunMarketLogisticsTrackingProvider(configuredProperties(), http)
                .query(new LogisticsTrackingQuery("DBKD", "DPK212715395868", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getCode()).isEqualTo(502);
                    assertThat(businessException.getMessage()).isEqualTo("物流查询服务返回无效数据")
                            .doesNotContain("raw-provider-message");
                });
    }

    private static AliyunMarketLogisticsProperties configuredProperties() {
        AliyunMarketLogisticsProperties properties = new AliyunMarketLogisticsProperties();
        properties.setEnabled(true);
        properties.setAppCode("test-app-code");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static String successBody() {
        return """
                {
                  "success": true,
                  "data": {
                    "cpCode": "DBKD",
                    "mailNo": "DPK212715395868",
                    "theLastTime": "2026-08-11 13:37:26",
                    "theLastMessage": "正常签收",
                    "logisticsCompanyName": "德邦快递",
                    "logisticsStatus": "SIGN",
                    "logisticsStatusDesc": "已签收",
                    "logisticsTraceDetailList": [
                      {"time": 1786421576000, "logisticsStatus": "TRANSPORT", "subLogisticsStatus": "TRANSPORT", "desc": "运输中", "areaName": "武汉市"},
                      {"time": 1786426646000, "logisticsStatus": "SIGN", "subLogisticsStatus": "SIGN", "desc": "已签收", "areaName": "鹰潭市"}
                    ]
                  }
                }
                """;
    }
}
