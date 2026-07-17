package my.hive.domain.order.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.infrastructure.logistics.Kuaidi100Client;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderLogisticsTrackingService {

    private static final String PROVIDER = "kuaidi100";
    private static final String ACTION = "realtime-query";
    private static final String FAILURE_ACTION = "realtime-query-error";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration FAILURE_CACHE_TTL = Duration.ofSeconds(30);
    private static final Set<String> PHONE_REQUIRED_COMPANIES = Set.of(
            "shunfeng", "shunfengkuaiyun", "zhongtong");
    private static final Pattern DIRECT_COMPANY_CODE = Pattern.compile("^[a-z][a-z0-9]{1,31}$");
    private static final Map<String, String> COMPANY_CODES = Map.ofEntries(
            Map.entry("顺丰", "shunfeng"),
            Map.entry("顺丰速运", "shunfeng"),
            Map.entry("顺丰快递", "shunfeng"),
            Map.entry("顺丰快运", "shunfengkuaiyun"),
            Map.entry("中通", "zhongtong"),
            Map.entry("中通快递", "zhongtong"),
            Map.entry("圆通", "yuantong"),
            Map.entry("圆通速递", "yuantong"),
            Map.entry("韵达", "yunda"),
            Map.entry("韵达快递", "yunda"),
            Map.entry("申通", "shentong"),
            Map.entry("申通快递", "shentong"),
            Map.entry("邮政ems", "ems"),
            Map.entry("ems", "ems"),
            Map.entry("京东", "jingdong"),
            Map.entry("京东物流", "jingdong"),
            Map.entry("德邦", "debangwuliu"),
            Map.entry("德邦物流", "debangwuliu"),
            Map.entry("极兔", "jtexpress"),
            Map.entry("极兔速递", "jtexpress"),
            Map.entry("跨越速运", "kuayue"),
            Map.entry("安能物流", "annengwuliu"),
            Map.entry("百世快递", "huitongkuaidi")
    );

    private final OrderService orderService;
    private final OrderShipmentService orderShipmentService;
    private final Kuaidi100Client kuaidi100Client;
    private final ExternalApiGuardService externalApiGuardService;
    private final ConcurrentHashMap<String, Object> queryLocks = new ConcurrentHashMap<>();

    public OrderLogisticsTrackingVO getTracking(String orderId, Long shipmentId) {
        if (orderId == null || orderId.isBlank()) {
            throw new BusinessException("订单编号不能为空");
        }
        orderId = orderId.trim();
        SalesOrder order = orderService.getSalesOrderForLogisticsTracking(orderId);
        SalesOrderShipment shipment = orderShipmentService.requireShipment(
                order.getTenantCode(), orderId, shipmentId);
        if (!Objects.equals(order.getTenantCode(), shipment.getTenantCode())
                || !Objects.equals(orderId, shipment.getOrderId())
                || !Objects.equals(shipmentId, shipment.getId())) {
            throw new BusinessException("Shipment does not exist or does not belong to this order");
        }
        String company = required(shipment.getLogisticsCompany(), "Shipment logistics company is required");
        String expressNo = required(shipment.getTrackingNo(), "Shipment tracking number is required");
        String companyCode = resolveCompanyCode(company);
        String cacheSource = String.join("|", order.getTenantCode(), orderId,
                String.valueOf(shipmentId), company, expressNo);
        String cacheKey = externalApiGuardService.fingerprint(cacheSource);

        OrderLogisticsTrackingVO cached = readCachedTracking(cacheKey);
        if (cached != null) {
            return cached;
        }

        Object queryLock = queryLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        try {
            synchronized (queryLock) {
                cached = readCachedTracking(cacheKey);
                if (cached != null) {
                    return cached;
                }
                throwCachedFailure(cacheKey);
                return queryAndCache(order, company, companyCode, expressNo, cacheKey);
            }
        } finally {
            queryLocks.remove(cacheKey, queryLock);
        }
    }

    private OrderLogisticsTrackingVO queryAndCache(SalesOrder order,
                                                    String company,
                                                    String companyCode,
                                                    String expressNo,
                                                    String cacheKey) {
        long startedAt = System.nanoTime();
        try {
            String phone = PHONE_REQUIRED_COMPANIES.contains(companyCode)
                    ? blankToNull(order.getCustomerPhone())
                    : null;
            OrderLogisticsTrackingVO result = kuaidi100Client.query(
                    companyCode,
                    expressNo,
                    phone);
            result.setCompany(company);
            result.setCompanyCode(companyCode);
            result.setExpressNo(expressNo);
            result.setCached(false);
            if (result.getQueriedAt() == null) {
                result.setQueriedAt(Instant.now());
            }
            result.setCacheExpiresAt(result.getQueriedAt().plus(CACHE_TTL));
            externalApiGuardService.evictCachedResponse(PROVIDER, FAILURE_ACTION, cacheKey);
            externalApiGuardService.cacheResponse(
                    PROVIDER,
                    ACTION,
                    cacheKey,
                    JSON.toJSONString(result),
                    CACHE_TTL);
            externalApiGuardService.recordCallEvent(
                    PROVIDER,
                    ACTION,
                    "SUCCESS",
                    order.getTenantCode(),
                    200,
                    elapsedMillis(startedAt),
                    "kuaidi100 realtime query succeeded",
                    Map.of("trackingFingerprint", externalApiGuardService.fingerprint(expressNo)));
            return result;
        } catch (BusinessException exception) {
            int failureCode = exception.getCode() == null ? 502 : exception.getCode();
            String failureMessage = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "快递查询失败，请稍后重试"
                    : exception.getMessage();
            externalApiGuardService.cacheResponse(
                    PROVIDER,
                    FAILURE_ACTION,
                    cacheKey,
                    JSON.toJSONString(Map.of("code", failureCode, "message", failureMessage)),
                    FAILURE_CACHE_TTL);
            externalApiGuardService.recordCallEvent(
                    PROVIDER,
                    ACTION,
                    "ERROR",
                    order.getTenantCode(),
                    exception.getCode(),
                    elapsedMillis(startedAt),
                    "kuaidi100 realtime query failed",
                    Map.of("trackingFingerprint", externalApiGuardService.fingerprint(expressNo)));
            throw exception;
        }
    }

    private OrderLogisticsTrackingVO readCachedTracking(String cacheKey) {
        String cached = externalApiGuardService.getCachedResponse(PROVIDER, ACTION, cacheKey);
        if (cached == null || cached.isBlank()) {
            return null;
        }
        try {
            OrderLogisticsTrackingVO result = JSON.parseObject(cached, OrderLogisticsTrackingVO.class);
            if (result != null) {
                result.setCached(true);
                return result;
            }
        } catch (Exception ignored) {
            // Invalid cache entries are discarded and refreshed from the provider.
        }
        externalApiGuardService.evictCachedResponse(PROVIDER, ACTION, cacheKey);
        return null;
    }

    private void throwCachedFailure(String cacheKey) {
        String cached = externalApiGuardService.getCachedResponse(PROVIDER, FAILURE_ACTION, cacheKey);
        if (cached == null || cached.isBlank()) {
            return;
        }
        try {
            JSONObject failure = JSON.parseObject(cached);
            Integer code = failure == null ? null : failure.getInteger("code");
            String message = failure == null ? null : failure.getString("message");
            if (message != null && !message.isBlank()) {
                throw new BusinessException(code == null ? 502 : code, message);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception ignored) {
            // Invalid cooldown entries should not block a fresh provider request.
        }
        externalApiGuardService.evictCachedResponse(PROVIDER, FAILURE_ACTION, cacheKey);
    }

    static String resolveCompanyCode(String company) {
        String normalized = company == null ? "" : company.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "");
        String mapped = COMPANY_CODES.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (DIRECT_COMPANY_CODE.matcher(normalized).matches()) {
            return normalized;
        }
        throw new BusinessException("无法识别物流公司，请填写快递100公司编码");
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
