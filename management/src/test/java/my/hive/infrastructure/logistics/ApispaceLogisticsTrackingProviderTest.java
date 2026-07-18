package my.hive.infrastructure.logistics;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApispaceLogisticsTrackingProviderTest {

    private static final String TEST_TOKEN = "test-token";

    @Test
    void postsTheApispaceRequestAndNormalizesTracesNewestFirst() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, successBody());
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(apiResponse);
        ApispaceLogisticsTrackingProvider provider = new ApispaceLogisticsTrackingProvider(configuredProperties(), http);

        OrderLogisticsTrackingVO result = provider.query(new LogisticsTrackingQuery("DBKD", "DPK366013054101", "0000"));

        assertThat(provider.providerCode()).isEqualTo("apispace");
        assertThat(result.getCompany()).isEqualTo("Deppon Express");
        assertThat(result.getCompanyCode()).isEqualTo("DBKD");
        assertThat(result.getTrackingNo()).isEqualTo("DPK366013054101");
        assertThat(result.getState()).isEqualTo("SIGN");
        assertThat(result.getStateLabel()).isEqualTo("Provider status summary");
        assertThat(result.getLatestContext()).isEqualTo("Provider latest message");
        assertThat(result.getLatestTime()).isEqualTo("Provider latest time");
        assertThat(result.getTraces()).extracting(OrderLogisticsTrackingVO.TraceVO::getContext)
                .containsExactly("Delivered", "Out for delivery", "Accepted");
        assertThat(result.getTraces()).extracting(OrderLogisticsTrackingVO.TraceVO::getTime)
                .containsExactly("1970-01-01 08:00:03", "1970-01-01 08:00:02", "1970-01-01 08:00:01");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri().toString())
                .isEqualTo("https://eolink.o.apispace.com/wlgj1/paidtobuy_api/trace_search");
        assertThat(request.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(request.headers().firstValue("X-APISpace-Token")).contains(TEST_TOKEN);
        JSONObject body = JSON.parseObject(requestBody(request));
        assertThat(body.getString("cpCode")).isEqualTo("DBKD");
        assertThat(body.getString("mailNo")).isEqualTo("DPK366013054101");
        assertThat(body.getString("tel")).isEqualTo("0000");
        assertThat(body.getString("orderType")).isEqualTo("asc");
    }

    @Test
    void rejectsDisabledAndBlankTokenConfiguration() {
        ApispaceLogisticsProperties disabled = new ApispaceLogisticsProperties();
        assertUnavailable(disabled);

        ApispaceLogisticsProperties blankToken = configuredProperties();
        blankToken.setToken("   ");
        assertUnavailable(blankToken);
    }

    @Test
    void fallsBackToTraceAndStaticSummaryValuesWhenProviderSummaryFieldsAreBlank() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, successBody()
                .replace("Provider status summary", "   ")
                .replace("Provider latest message", "   ")
                .replace("Provider latest time", "   "));
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(apiResponse);

        OrderLogisticsTrackingVO result = new ApispaceLogisticsTrackingProvider(configuredProperties(), http)
                .query(new LogisticsTrackingQuery("DBKD", "DPK366013054101", null));

        assertThat(result.getStateLabel()).isNotBlank().isNotEqualTo("Provider status summary");
        assertThat(result.getLatestContext()).isEqualTo("Delivered");
        assertThat(result.getLatestTime()).isEqualTo("1970-01-01 08:00:03");
    }

    @ParameterizedTest
    @MethodSource("httpFailures")
    void mapsHttpFailuresWithoutLeakingProviderData(int status, int expectedCode, String expectedMessage) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(status, "provider-body-" + TEST_TOKEN);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(apiResponse);

        assertSanitizedFailure(new ApispaceLogisticsTrackingProvider(configuredProperties(), http), expectedCode,
                expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsProviderFailuresWithoutLeakingProviderData(String body) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> apiResponse = response(200, body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(apiResponse);

        assertSanitizedFailure(new ApispaceLogisticsTrackingProvider(configuredProperties(), http), 502,
                "物流查询服务返回无效数据");
    }

    private void assertUnavailable(ApispaceLogisticsProperties properties) {
        assertThatThrownBy(() -> new ApispaceLogisticsTrackingProvider(properties, mock(HttpClient.class))
                .query(new LogisticsTrackingQuery("DBKD", "DPK366013054101", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);
    }

    private void assertSanitizedFailure(ApispaceLogisticsTrackingProvider provider, int expectedCode,
                                        String expectedMessage) {
        assertThatThrownBy(() -> provider.query(new LogisticsTrackingQuery("DBKD", "DPK366013054101", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(expectedCode);
                    assertThat(businessException.getMessage()).isEqualTo(expectedMessage)
                            .doesNotContain("provider-body", TEST_TOKEN, "raw-provider-message");
                });
    }

    private static Stream<Arguments> httpFailures() {
        return Stream.of(
                Arguments.of(401, 503, "APISpace物流查询鉴权失败"),
                Arguments.of(403, 503, "APISpace物流查询鉴权失败"),
                Arguments.of(413, 429, "APISpace物流查询频率过高，请稍后重试"),
                Arguments.of(416, 503, "APISpace物流查询服务未开通或配额不足"),
                Arguments.of(504, 502, "物流查询服务暂时不可用"));
    }

    private static Stream<String> invalidBodies() {
        return Stream.of(
                "{\"success\":false,\"message\":\"raw-provider-message-test-token\"}",
                "{\"success\":true,\"message\":\"raw-provider-message-test-token\"}",
                "not-json-test-token");
    }

    private ApispaceLogisticsProperties configuredProperties() {
        ApispaceLogisticsProperties properties = new ApispaceLogisticsProperties();
        properties.setEnabled(true);
        properties.setToken(TEST_TOKEN);
        return properties;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private String requestBody(HttpRequest request) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompletableFuture<Void> complete = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                ByteBuffer copy = buffer.asReadOnlyBuffer();
                byte[] bytes = new byte[copy.remaining()];
                copy.get(bytes);
                output.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                complete.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                complete.complete(null);
            }
        });
        complete.join();
        return output.toString(StandardCharsets.UTF_8);
    }

    private String successBody() {
        return """
                {
                  "success": true,
                  "logisticsTrace": {
                    "cpCode": "DBKD",
                    "mailNo": "DPK366013054101",
                    "theLastTime": "Provider latest time",
                    "theLastMessage": "Provider latest message",
                    "logisticsCompanyName": "Deppon Express",
                    "logisticsStatus": "SIGN",
                    "logisticsStatusDesc": "Provider status summary",
                    "logisticsTraceDetailList": [
                      {"time": 1000, "logisticsStatus": "ACCEPT", "desc": "Accepted", "areaName": "Origin"},
                      {"time": 3000, "logisticsStatus": "SIGN", "desc": "Delivered", "areaName": "Destination"},
                      {"time": 2000, "logisticsStatus": "DELIVERING", "desc": "Out for delivery", "areaName": "Transit"}
                    ]
                  }
                }
                """;
    }
}
