package my.hive.infrastructure.logistics;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
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

@Component
public class ApispaceLogisticsTrackingProvider implements LogisticsTrackingProvider {

    private static final URI QUERY_URI = URI.create("https://eolink.o.apispace.com/wlgj1/paidtobuy_api/trace_search");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(BEIJING_ZONE);

    private final ApispaceLogisticsProperties properties;
    private final HttpClient http;

    @Autowired
    public ApispaceLogisticsTrackingProvider(ApispaceLogisticsProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build());
    }

    ApispaceLogisticsTrackingProvider(ApispaceLogisticsProperties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public String providerCode() {
        return "apispace";
    }

    @Override
    public OrderLogisticsTrackingVO query(LogisticsTrackingQuery query) {
        requireConfigured();
        HttpRequest request = HttpRequest.newBuilder(QUERY_URI)
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", "application/json")
                .header("X-APISpace-Token", properties.getToken().trim())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(query), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpFailure(response.statusCode());
            }
            return normalize(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "物流查询服务暂时不可用");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(502, "物流查询服务暂时不可用");
        }
    }

    private String requestBody(LogisticsTrackingQuery query) {
        JSONObject body = new JSONObject();
        body.put("cpCode", query.companyCode());
        body.put("mailNo", query.trackingNo());
        body.put("tel", query.phoneSuffix() == null ? "" : query.phoneSuffix());
        body.put("orderType", "asc");
        return JSON.toJSONString(body);
    }

    private OrderLogisticsTrackingVO normalize(String body) {
        try {
            JSONObject response = JSON.parseObject(body);
            if (response == null || !Boolean.TRUE.equals(response.getBoolean("success"))) {
                throw providerFailure();
            }
            JSONObject logisticsTrace = response.getJSONObject("logisticsTrace");
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

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void requireConfigured() {
        if (!properties.isEnabled() || properties.getToken() == null || properties.getToken().isBlank()) {
            throw new BusinessException(503, "APISpace物流查询未配置");
        }
    }

    private BusinessException httpFailure(int status) {
        return switch (status) {
            case 401, 403 -> new BusinessException(503, "APISpace物流查询鉴权失败");
            case 413 -> new BusinessException(429, "APISpace物流查询频率过高，请稍后重试");
            case 416 -> new BusinessException(503, "APISpace物流查询服务未开通或配额不足");
            case 504 -> new BusinessException(502, "物流查询服务暂时不可用");
            default -> new BusinessException(502, "物流查询服务暂时不可用");
        };
    }

    private BusinessException providerFailure() {
        return new BusinessException(502, "物流查询服务返回无效数据");
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
