package my.hive.domain.order.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.infrastructure.logistics.LogisticsTrackingGateway;
import my.hive.infrastructure.logistics.LogisticsTrackingQuery;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderLogisticsTrackingService {

    private static final String ACTION = "realtime-query";
    private static final String FAILURE_ACTION = "realtime-query-error";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration FAILURE_CACHE_TTL = Duration.ofSeconds(30);
    private static final Pattern DIRECT_COMPANY_CODE = Pattern.compile("^[A-Z][A-Z0-9_-]{1,31}$");
    private static final Map<String, String> COMPANY_CODES = Map.ofEntries(
            Map.entry("顺丰", "SF"),
            Map.entry("顺丰速运", "SF"),
            Map.entry("顺丰快递", "SF"),
            Map.entry("顺丰快运", "SFKY"),
            Map.entry("中通", "ZTO"),
            Map.entry("中通快递", "ZTO"),
            Map.entry("圆通", "YTO"),
            Map.entry("圆通速递", "YTO"),
            Map.entry("韵达", "YUNDA"),
            Map.entry("韵达快递", "YUNDA"),
            Map.entry("申通", "STO"),
            Map.entry("申通快递", "STO"),
            Map.entry("邮政ems", "EMS"),
            Map.entry("ems", "EMS"),
            Map.entry("京东", "JD"),
            Map.entry("京东物流", "JD"),
            Map.entry("德邦", "DBKD"),
            Map.entry("德邦物流", "DBKD"),
            Map.entry("极兔", "JTSD"),
            Map.entry("极兔速递", "JTSD"),
            Map.entry("跨越速运", "KYE"),
            Map.entry("安能物流", "ANE56"),
            Map.entry("百世快递", "HTKY")
    );

    private final OrderService orderService;
    private final OrderShipmentService orderShipmentService;
    private final LogisticsTrackingGateway logisticsTrackingGateway;
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
        String trackingNo = required(shipment.getTrackingNo(), "Shipment tracking number is required");
        String companyCode = resolveCompanyCode(company);
        String cacheSource = String.join("|", order.getTenantCode(), orderId,
                String.valueOf(shipmentId), company, trackingNo);
        String cacheKey = externalApiGuardService.fingerprint(cacheSource);
        String provider = logisticsTrackingGateway.providerCode() + "-logistics";

        OrderLogisticsTrackingVO cached = readCachedTracking(provider, cacheKey);
        if (cached != null) {
            return cached;
        }

        Object queryLock = queryLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        try {
            synchronized (queryLock) {
                cached = readCachedTracking(provider, cacheKey);
                if (cached != null) {
                    return cached;
                }
                throwCachedFailure(provider, cacheKey);
                return queryAndCache(order, company, companyCode, trackingNo, provider, cacheKey);
            }
        } finally {
            queryLocks.remove(cacheKey, queryLock);
        }
    }

    private OrderLogisticsTrackingVO queryAndCache(SalesOrder order,
                                                    String company,
                                                    String companyCode,
                                                    String trackingNo,
                                                    String provider,
                                                    String cacheKey) {
        long startedAt = System.nanoTime();
        try {
            String phoneSuffix = phoneSuffix(order.getCustomerPhone());
            OrderLogisticsTrackingVO result = logisticsTrackingGateway.query(
                    new LogisticsTrackingQuery(companyCode, trackingNo, phoneSuffix));
            result.setCompany(company);
            result.setCompanyCode(companyCode);
            result.setTrackingNo(trackingNo);
            result.setCached(false);
            if (result.getQueriedAt() == null) {
                result.setQueriedAt(Instant.now());
            }
            result.setCacheExpiresAt(result.getQueriedAt().plus(CACHE_TTL));
            externalApiGuardService.evictCachedResponse(provider, FAILURE_ACTION, cacheKey);
            externalApiGuardService.cacheResponse(
                    provider,
                    ACTION,
                    cacheKey,
                    JSON.toJSONString(result),
                    CACHE_TTL);
            externalApiGuardService.recordCallEvent(
                    provider,
                    ACTION,
                    "SUCCESS",
                    order.getTenantCode(),
                    200,
                    elapsedMillis(startedAt),
                    "logistics realtime query succeeded",
                    Map.of("trackingFingerprint", externalApiGuardService.fingerprint(trackingNo)));
            return result;
        } catch (BusinessException exception) {
            int failureCode = exception.getCode() == null ? 502 : exception.getCode();
            String failureMessage = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "快递查询失败，请稍后重试"
                    : exception.getMessage();
            externalApiGuardService.cacheResponse(
                    provider,
                    FAILURE_ACTION,
                    cacheKey,
                    JSON.toJSONString(Map.of("code", failureCode, "message", failureMessage)),
                    FAILURE_CACHE_TTL);
            externalApiGuardService.recordCallEvent(
                    provider,
                    ACTION,
                    "ERROR",
                    order.getTenantCode(),
                    exception.getCode(),
                    elapsedMillis(startedAt),
                    "logistics realtime query failed",
                    Map.of("trackingFingerprint", externalApiGuardService.fingerprint(trackingNo)));
            throw exception;
        }
    }

    private OrderLogisticsTrackingVO readCachedTracking(String provider, String cacheKey) {
        String cached = externalApiGuardService.getCachedResponse(provider, ACTION, cacheKey);
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
        externalApiGuardService.evictCachedResponse(provider, ACTION, cacheKey);
        return null;
    }

    private void throwCachedFailure(String provider, String cacheKey) {
        String cached = externalApiGuardService.getCachedResponse(provider, FAILURE_ACTION, cacheKey);
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
        externalApiGuardService.evictCachedResponse(provider, FAILURE_ACTION, cacheKey);
    }

    static String resolveCompanyCode(String company) {
        String normalized = company == null ? "" : company.trim().replace(" ", "");
        String mapped = COMPANY_CODES.get(normalized.toLowerCase(Locale.ROOT));
        if (mapped != null) {
            return mapped;
        }
        String directCode = normalized.toUpperCase(Locale.ROOT);
        if (DIRECT_COMPANY_CODE.matcher(directCode).matches()) {
            return directCode;
        }
        throw new BusinessException("无法识别物流公司，请填写物流供应商公司编码");
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private static String phoneSuffix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.length() < 4 ? null : digits.substring(digits.length() - 4);
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
