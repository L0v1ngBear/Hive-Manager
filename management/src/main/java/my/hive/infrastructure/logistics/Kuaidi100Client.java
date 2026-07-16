package my.hive.infrastructure.logistics;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class Kuaidi100Client {

    private static final URI QUERY_URI = URI.create("https://poll.kuaidi100.com/poll/query.do");

    private final Kuaidi100Properties properties;
    private final HttpClient http;

    @Autowired
    public Kuaidi100Client(Kuaidi100Properties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build());
    }

    Kuaidi100Client(Kuaidi100Properties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    public OrderLogisticsTrackingVO query(String companyCode, String expressNo, String phone) {
        requireConfigured();
        if (isBlank(companyCode) || isBlank(expressNo) || expressNo.trim().length() < 6 || expressNo.trim().length() > 32) {
            throw new BusinessException(400, "物流公司或物流单号格式不正确");
        }
        String param = buildParam(companyCode, expressNo, phone);
        String body = formBody(param, sign(param), properties.getCustomer());
        HttpRequest request = HttpRequest.newBuilder(QUERY_URI)
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "快递100查询服务暂时不可用");
            }
            JSONObject json = JSON.parseObject(response.body());
            if (json == null) {
                throw new BusinessException(502, "快递100返回了无效数据");
            }
            String status = json.getString("status");
            if (!"200".equals(status)) {
                throw providerFailure(status);
            }
            return normalize(json, companyCode, expressNo);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "快递100查询被中断，请稍后重试");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(502, "快递100查询失败，请稍后重试");
        }
    }

    static String buildParam(String companyCode, String expressNo, String phone) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("com", companyCode);
        param.put("num", expressNo);
        if (phone != null && !phone.isBlank()) {
            param.put("phone", phone.trim());
        }
        param.put("resultv2", 4);
        param.put("show", "0");
        param.put("order", "desc");
        param.put("lang", "zh");
        return JSON.toJSONString(param);
    }

    String sign(String param) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest((param + properties.getKey() + properties.getCustomer())
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                value.append(String.format("%02x", item));
            }
            return value.toString().toUpperCase(Locale.ROOT);
        } catch (Exception exception) {
            throw new BusinessException(500, "快递查询签名生成失败");
        }
    }

    static String formBody(String param, String sign, String customer) {
        return "customer=" + encode(customer)
                + "&sign=" + encode(sign)
                + "&param=" + encode(param);
    }

    private OrderLogisticsTrackingVO normalize(JSONObject json, String companyCode, String expressNo) {
        OrderLogisticsTrackingVO result = new OrderLogisticsTrackingVO();
        result.setCompanyCode(companyCode);
        result.setExpressNo(expressNo);
        result.setState(json.getString("state"));
        result.setStateLabel(stateLabel(result.getState()));
        result.setQueriedAt(Instant.now());

        JSONArray data = json.getJSONArray("data");
        if (data != null) {
            for (int index = 0; index < data.size(); index++) {
                JSONObject source = data.getJSONObject(index);
                if (source == null) {
                    continue;
                }
                OrderLogisticsTrackingVO.TraceVO trace = new OrderLogisticsTrackingVO.TraceVO();
                trace.setContext(source.getString("context"));
                trace.setTime(firstNotBlank(source.getString("ftime"), source.getString("time")));
                trace.setStatus(source.getString("status"));
                trace.setStatusCode(source.getString("statusCode"));
                trace.setLocation(firstNotBlank(source.getString("location"), source.getString("areaName")));
                result.getTraces().add(trace);
            }
        }
        if (!result.getTraces().isEmpty()) {
            OrderLogisticsTrackingVO.TraceVO latest = result.getTraces().get(0);
            result.setLatestContext(latest.getContext());
            result.setLatestTime(latest.getTime());
        }
        return result;
    }

    private BusinessException providerFailure(String status) {
        return switch (status == null ? "" : status) {
            case "408" -> new BusinessException(400, "该快递查询需要订单客户手机号，请先补充手机号");
            case "400" -> new BusinessException(400, "物流公司或物流单号不完整，无法查询");
            case "500" -> new BusinessException(404, "暂未查询到物流轨迹");
            case "501", "502" -> new BusinessException(502, "快递100查询服务暂时不可用");
            case "503" -> new BusinessException(502, "快递100鉴权失败，请检查服务器配置");
            case "601" -> new BusinessException(503, "快递100授权已过期，请更新服务器配置");
            default -> new BusinessException(502, "快递100查询失败，请稍后重试");
        };
    }

    private void requireConfigured() {
        if (!properties.isEnabled()
                || isBlank(properties.getKey())
                || isBlank(properties.getCustomer())) {
            throw new BusinessException(503, "快递100物流查询未配置");
        }
    }

    private static String stateLabel(String state) {
        return switch (state == null ? "" : state) {
            case "0" -> "运输中";
            case "1" -> "已揽收";
            case "2" -> "物流异常";
            case "3" -> "已签收";
            case "4" -> "已退签";
            case "5" -> "派送中";
            case "6" -> "退回中";
            case "7" -> "转投中";
            case "10" -> "待清关";
            case "11" -> "清关中";
            case "12" -> "已清关";
            case "13" -> "清关异常";
            case "14" -> "已拒签";
            default -> "物流状态已更新";
        };
    }

    private static String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
