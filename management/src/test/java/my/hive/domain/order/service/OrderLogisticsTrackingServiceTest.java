package my.hive.domain.order.service;

import com.alibaba.fastjson2.JSON;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.infrastructure.logistics.LogisticsTrackingGateway;
import my.hive.infrastructure.logistics.LogisticsTrackingQuery;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLogisticsTrackingServiceTest {

    @Test
    void queriesShipmentCourierAndCachesByTenantOrderShipmentCompanyAndTrackingNumber() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        SalesOrder order = order();
        SalesOrderShipment shipment = shipment(7L, "  \u987a\u4e30\u901f\u8fd0  ", "  SF123456  ");
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order);
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L)).thenReturn(shipment);
        when(guard.fingerprint("TENANT_001|SO-001|7|\u987a\u4e30\u901f\u8fd0|SF123456"))
                .thenReturn("cache-fingerprint");
        when(guard.fingerprint("SF123456")).thenReturn("tracking-fingerprint");
        when(gateway.providerCode()).thenReturn("apispace");
        when(guard.getCachedResponse("apispace-logistics", "realtime-query", "cache-fingerprint")).thenReturn(null);

        OrderLogisticsTrackingVO providerResult = new OrderLogisticsTrackingVO();
        providerResult.setState("0");
        providerResult.setTraces(List.of());
        when(gateway.query(new LogisticsTrackingQuery("SF", "SF123456", "0000"))).thenReturn(providerResult);

        OrderLogisticsTrackingService service = new OrderLogisticsTrackingService(
                orderService, shipmentService, gateway, guard);
        OrderLogisticsTrackingVO result = service.getTracking("SO-001", 7L);

        assertThat(result.getCompany()).isEqualTo("\u987a\u4e30\u901f\u8fd0");
        assertThat(result.getCompanyCode()).isEqualTo("SF");
        assertThat(result.getTrackingNo()).isEqualTo("SF123456");
        assertThat(result.isCached()).isFalse();
        assertThat(result.getCacheExpiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
        verify(shipmentService).requireShipment("TENANT_001", "SO-001", 7L);
        verify(guard).fingerprint("TENANT_001|SO-001|7|\u987a\u4e30\u901f\u8fd0|SF123456");
        verify(gateway).query(new LogisticsTrackingQuery("SF", "SF123456", "0000"));

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(guard).cacheResponse(
                eq("apispace-logistics"), eq("realtime-query"), eq("cache-fingerprint"), anyString(), ttl.capture());
        assertThat(ttl.getValue()).isEqualTo(Duration.ofMinutes(30));

        ArgumentCaptor<Map<String, Object>> successEventDetail = ArgumentCaptor.forClass(Map.class);
        verify(guard).recordCallEvent(
                eq("apispace-logistics"), eq("realtime-query"), eq("SUCCESS"), eq("TENANT_001"), eq(200),
                any(), anyString(), successEventDetail.capture());
        assertThat(successEventDetail.getValue())
                .containsOnly(Map.entry("trackingFingerprint", "tracking-fingerprint"))
                .doesNotContainValue("13800000000")
                .doesNotContainValue("SF123456");
    }

    @Test
    void rejectsShipmentFromDifferentOrderOrTenantBeforeProvider() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order());
        OrderLogisticsTrackingService service = new OrderLogisticsTrackingService(
                orderService, shipmentService, gateway, guard);

        SalesOrderShipment otherOrder = shipment(7L, "shunfeng", "SF123456");
        otherOrder.setOrderId("SO-OTHER");
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L)).thenReturn(otherOrder);
        assertThatThrownBy(() -> service.getTracking("SO-001", 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");

        SalesOrderShipment otherTenant = shipment(8L, "shunfeng", "SF654321");
        otherTenant.setTenantCode("TENANT_OTHER");
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 8L)).thenReturn(otherTenant);
        assertThatThrownBy(() -> service.getTracking("SO-001", 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");

        verify(guard, never()).fingerprint(anyString());
        verify(gateway, never()).query(any(LogisticsTrackingQuery.class));
    }

    @Test
    void returnsCachedShipmentResponseWithoutCallingProvider() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order());
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L))
                .thenReturn(shipment(7L, "zhongtong", "ZT123456"));
        when(guard.fingerprint("TENANT_001|SO-001|7|zhongtong|ZT123456")).thenReturn("cache-fingerprint");

        OrderLogisticsTrackingVO cached = new OrderLogisticsTrackingVO();
        cached.setCompany("zhongtong");
        cached.setCompanyCode("zhongtong");
        cached.setTrackingNo("ZT123456");
        cached.setCacheExpiresAt(Instant.now().plus(Duration.ofMinutes(20)));
        cached.setTraces(List.of());
        when(gateway.providerCode()).thenReturn("apispace");
        when(guard.getCachedResponse("apispace-logistics", "realtime-query", "cache-fingerprint"))
                .thenReturn(JSON.toJSONString(cached));

        OrderLogisticsTrackingVO result = new OrderLogisticsTrackingService(
                orderService, shipmentService, gateway, guard).getTracking("SO-001", 7L);

        assertThat(result.isCached()).isTrue();
        verify(gateway, never()).query(any(LogisticsTrackingQuery.class));
    }

    @Test
    void rejectsUnsupportedShipmentCourierBeforeCallingProvider() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order());
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L))
                .thenReturn(shipment(7L, "private-fleet!", "CAR-001"));

        assertThatThrownBy(() -> new OrderLogisticsTrackingService(orderService, shipmentService, gateway, guard)
                .getTracking("SO-001", 7L))
                .isInstanceOf(BusinessException.class);
        verify(gateway, never()).query(any(LogisticsTrackingQuery.class));
    }

    @Test
    void sendsOnlyCustomerPhoneSuffixToProvider() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order());
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L))
                .thenReturn(shipment(7L, "韵达", "YD123456"));
        when(guard.fingerprint("TENANT_001|SO-001|7|韵达|YD123456")).thenReturn("cache-fingerprint");
        when(guard.fingerprint("YD123456")).thenReturn("tracking-fingerprint");
        when(gateway.providerCode()).thenReturn("apispace");
        when(gateway.query(new LogisticsTrackingQuery("YUNDA", "YD123456", "0000")))
                .thenReturn(new OrderLogisticsTrackingVO());

        new OrderLogisticsTrackingService(orderService, shipmentService, gateway, guard)
                .getTracking("SO-001", 7L);

        verify(gateway).query(new LogisticsTrackingQuery("YUNDA", "YD123456", "0000"));
    }

    @Test
    void reusesSanitizedShipmentFailureCooldownWithoutCallingProviderAgain() {
        OrderService orderService = mock(OrderService.class);
        OrderShipmentService shipmentService = mock(OrderShipmentService.class);
        LogisticsTrackingGateway gateway = mock(LogisticsTrackingGateway.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order());
        when(shipmentService.requireShipment("TENANT_001", "SO-001", 7L))
                .thenReturn(shipment(7L, "顺丰速运", "SF123456"));
        when(guard.fingerprint("TENANT_001|SO-001|7|顺丰速运|SF123456")).thenReturn("cache-fingerprint");
        when(guard.fingerprint("SF123456")).thenReturn("tracking-fingerprint");
        when(gateway.providerCode()).thenReturn("apispace");
        when(guard.getCachedResponse("apispace-logistics", "realtime-query", "cache-fingerprint")).thenReturn(null);
        when(guard.getCachedResponse("apispace-logistics", "realtime-query-error", "cache-fingerprint"))
                .thenReturn(null, "{\"code\":502,\"message\":\"provider unavailable\"}");
        when(gateway.query(new LogisticsTrackingQuery("SF", "SF123456", "0000")))
                .thenThrow(new BusinessException(502, "provider unavailable"));
        OrderLogisticsTrackingService service = new OrderLogisticsTrackingService(
                orderService, shipmentService, gateway, guard);

        assertThatThrownBy(() -> service.getTracking("SO-001", 7L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getTracking("SO-001", 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("provider unavailable");
        verify(gateway, times(1)).query(new LogisticsTrackingQuery("SF", "SF123456", "0000"));

        ArgumentCaptor<Duration> failureTtl = ArgumentCaptor.forClass(Duration.class);
        verify(guard).cacheResponse(
                eq("apispace-logistics"), eq("realtime-query-error"), eq("cache-fingerprint"), anyString(),
                failureTtl.capture());
        assertThat(failureTtl.getValue()).isEqualTo(Duration.ofSeconds(30));

        ArgumentCaptor<Map<String, Object>> errorEventDetail = ArgumentCaptor.forClass(Map.class);
        verify(guard).recordCallEvent(
                eq("apispace-logistics"), eq("realtime-query"), eq("ERROR"), eq("TENANT_001"), eq(502),
                any(), anyString(), errorEventDetail.capture());
        assertThat(errorEventDetail.getValue())
                .containsOnly(Map.entry("trackingFingerprint", "tracking-fingerprint"))
                .doesNotContainValue("13800000000")
                .doesNotContainValue("SF123456");
    }

    @Test
    void mapsSupportedCompaniesToApispaceCodes() {
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("顺丰速运")).isEqualTo("SF");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("中通快递")).isEqualTo("ZTO");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("圆通速递")).isEqualTo("YTO");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("韵达快递")).isEqualTo("YUNDA");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("申通快递")).isEqualTo("STO");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("EMS")).isEqualTo("EMS");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("京东物流")).isEqualTo("JD");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("德邦物流")).isEqualTo("DBKD");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("极兔速递")).isEqualTo("JTSD");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("跨越速运")).isEqualTo("KYE");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("安能物流")).isEqualTo("ANE56");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("百世快递")).isEqualTo("HTKY");
        assertThat(OrderLogisticsTrackingService.resolveCompanyCode("YTO")).isEqualTo("YTO");
    }

    private SalesOrder order() {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-001");
        order.setTenantCode("TENANT_001");
        order.setStatus("shipped");
        order.setCustomerPhone("13800000000");
        return order;
    }

    private SalesOrderShipment shipment(Long id, String company, String trackingNo) {
        SalesOrderShipment shipment = new SalesOrderShipment();
        shipment.setId(id);
        shipment.setTenantCode("TENANT_001");
        shipment.setOrderId("SO-001");
        shipment.setLogisticsCompany(company);
        shipment.setTrackingNo(trackingNo);
        return shipment;
    }
}
