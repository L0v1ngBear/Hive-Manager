package my.hive.infrastructure.logistics;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.external.ExternalApiResponseDiagnosticLogger;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aliyun Market express tracking API: cmapi00066593.
 */
@Component
public class AliyunMarketLogisticsTrackingProvider implements LogisticsTrackingProvider {

    private static final String QUERY_ENDPOINT = "https://kzexpress.market.alicloudapi.com/api-mall/api/express/query";
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(BEIJING_ZONE);

    private final AliyunMarketLogisticsProperties properties;
    private final HttpClient http;
    private final ExternalApiResponseDiagnosticLogger diagnosticLogger;

    @Autowired
    public AliyunMarketLogisticsTrackingProvider(AliyunMarketLogisticsProperties properties,
                                                 ExternalApiResponseDiagnosticLogger diagnosticLogger) {
        this(properties, HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build(), diagnosticLogger);
    }

    AliyunMarketLogisticsTrackingProvider(AliyunMarketLogisticsProperties properties, HttpClient http) {
        this(properties, http, null);
    }

    AliyunMarketLogisticsTrackingProvider(AliyunMarketLogisticsProperties properties,
                                          HttpClient http,
                                          ExternalApiResponseDiagnosticLogger diagnosticLogger) {
        this.properties = properties;
        this.http = http;
        this.diagnosticLogger = diagnosticLogger;
    }

    @Override
    public String providerCode() {
        return "aliyun-market";
    }

    @Override
    public boolean supportsCompanyCodeAutoRecognition() {
        return true;
    }

    @Override
    public OrderLogisticsTrackingVO query(LogisticsTrackingQuery query) {
        requireConfigured();
        HttpRequest request = HttpRequest.newBuilder(queryUri(query))
                .timeout(properties.getRequestTimeout())
                .header("Authorization", "APPCODE " + properties.getAppCode().trim())
                .GET()
                .build();
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            recordResponse(response, elapsedMillis(startedAt));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpFailure(response.statusCode());
            }
            return normalize(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordTransportFailure(elapsedMillis(startedAt), exception);
            throw new BusinessException(502, "物流查询服务暂时不可用");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            recordTransportFailure(elapsedMillis(startedAt), exception);
            throw new BusinessException(502, "物流查询服务暂时不可用");
        }
    }

    private void recordResponse(HttpResponse<String> response, long durationMillis) {
        if (diagnosticLogger != null) {
            diagnosticLogger.recordResponse(providerCode(), "trace-query", response.statusCode(), durationMillis, response.body());
        }
    }

    private void recordTransportFailure(long durationMillis, Throwable exception) {
        if (diagnosticLogger != null) {
            diagnosticLogger.recordTransportFailure(providerCode(), "trace-query", durationMillis, exception);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private URI queryUri(LogisticsTrackingQuery query) {
        StringBuilder uri = new StringBuilder(QUERY_ENDPOINT)
                .append("?expressNo=").append(encode(query.trackingNo()));
        if (nonBlank(query.phoneSuffix()) != null) {
            uri.append("&mobile=").append(encode(query.phoneSuffix()));
        }
        if (nonBlank(query.companyCode()) != null) {
            uri.append("&cpCode=").append(encode(query.companyCode()));
        }
        return URI.create(uri.toString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private OrderLogisticsTrackingVO normalize(String body) {
        try {
            JSONObject response = JSON.parseObject(body);
            if (response == null || !Boolean.TRUE.equals(response.getBoolean("success"))) {
                throw providerFailure();
            }
            JSONObject logisticsTrace = response.getJSONObject("data");
            if (logisticsTrace == null) {
                throw providerFailure();
            }
            return toTracking(logisticsTrace);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw providerFailure();
        }
    }

    private OrderLogisticsTrackingVO toTracking(JSONObject logisticsTrace) {
        OrderLogisticsTrackingVO result = new OrderLogisticsTrackingVO();
        result.setCompany(logisticsTrace.getString("logisticsCompanyName"));
        result.setCompanyCode(logisticsTrace.getString("cpCode"));
        result.setTrackingNo(logisticsTrace.getString("mailNo"));
        result.setState(logisticsTrace.getString("logisticsStatus"));
        String providerStateLabel = nonBlank(logisticsTrace.getString("logisticsStatusDesc"));
        result.setStateLabel(providerStateLabel == null ? stateLabel(result.getState()) : providerStateLabel);
        result.setQueriedAt(Instant.now());

        List<TimedTrace> traces = new ArrayList<>();
        JSONArray details = logisticsTrace.getJSONArray("logisticsTraceDetailList");
        if (details != null) {
            for (int index = 0; index < details.size(); index++) {
                JSONObject detail = details.getJSONObject(index);
                if (detail == null) {
                    continue;
                }
                Long timestamp = detail.getLong("time");
                if (timestamp == null) {
                    continue;
                }
                OrderLogisticsTrackingVO.TraceVO trace = new OrderLogisticsTrackingVO.TraceVO();
                trace.setContext(detail.getString("desc"));
                trace.setTime(TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp)));
                trace.setStatus(detail.getString("logisticsStatus"));
                trace.setStatusCode(detail.getString("subLogisticsStatus"));
                trace.setLocation(detail.getString("areaName"));
                traces.add(new TimedTrace(timestamp, trace));
            }
        }
        traces.sort(Comparator.comparingLong(TimedTrace::timestamp).reversed());
        for (TimedTrace trace : traces) {
            result.getTraces().add(trace.value());
        }
        String providerLatestContext = nonBlank(logisticsTrace.getString("theLastMessage"));
        String providerLatestTime = nonBlank(logisticsTrace.getString("theLastTime"));
        if (providerLatestContext != null) {
            result.setLatestContext(providerLatestContext);
        }
        if (providerLatestTime != null) {
            result.setLatestTime(providerLatestTime);
        }
        if ((providerLatestContext == null || providerLatestTime == null) && !result.getTraces().isEmpty()) {
            OrderLogisticsTrackingVO.TraceVO latest = result.getTraces().get(0);
            if (providerLatestContext == null) {
                result.setLatestContext(latest.getContext());
            }
            if (providerLatestTime == null) {
                result.setLatestTime(latest.getTime());
            }
        }
        return result;
    }

    private void requireConfigured() {
        if (!properties.isEnabled() || properties.getAppCode() == null || properties.getAppCode().isBlank()) {
            throw new BusinessException(503, "阿里云物流查询未配置");
        }
    }

    private BusinessException httpFailure(int status) {
        return switch (status) {
            case 401, 403 -> new BusinessException(503, "阿里云物流查询鉴权失败");
            case 429 -> new BusinessException(429, "阿里云物流查询频率过高，请稍后重试");
            case 402 -> new BusinessException(503, "阿里云物流查询服务未开通或配额不足");
            default -> new BusinessException(502, "物流查询服务暂时不可用");
        };
    }

    private static BusinessException providerFailure() {
        return new BusinessException(502, "物流查询服务返回无效数据");
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String stateLabel(String state) {
        return switch (state == null ? "" : state) {
            case "SIGN" -> "已签收";
            case "DELIVERING" -> "派送中";
            case "TRANSPORT" -> "运输中";
            case "ACCEPT" -> "已揽收";
            case "FAILED" -> "物流异常";
            default -> "物流状态已更新";
        };
    }

    private record TimedTrace(long timestamp, OrderLogisticsTrackingVO.TraceVO value) {
    }
}
