package my.hive.domain.order.service;

import com.alibaba.fastjson2.JSON;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.infrastructure.logistics.Kuaidi100Client;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLogisticsTrackingServiceTest {

    @Test
    void queriesMappedCourierAndCachesNormalizedResponseForThirtyMinutes() {
        OrderService orderService = mock(OrderService.class);
        Kuaidi100Client client = mock(Kuaidi100Client.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        SalesOrder order = order("顺丰速运", "SF123456");
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order);
        when(guard.getCachedResponse(eq("kuaidi100"), eq("realtime-query"), anyString())).thenReturn(null);
        when(guard.fingerprint(anyString())).thenReturn("cache-fingerprint");

        OrderLogisticsTrackingVO providerResult = new OrderLogisticsTrackingVO();
        providerResult.setState("0");
        providerResult.setStateLabel("运输中");
        providerResult.setTraces(List.of());
        when(client.query("shunfeng", "SF123456", "13800000000")).thenReturn(providerResult);

        OrderLogisticsTrackingService service = new OrderLogisticsTrackingService(orderService, client, guard);
        OrderLogisticsTrackingVO result = service.getTracking("SO-001");

        assertThat(result.getCompany()).isEqualTo("顺丰速运");
        assertThat(result.getCompanyCode()).isEqualTo("shunfeng");
        assertThat(result.getExpressNo()).isEqualTo("SF123456");
        assertThat(result.isCached()).isFalse();
        assertThat(result.getCacheExpiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(guard).cacheResponse(eq("kuaidi100"), eq("realtime-query"), eq("cache-fingerprint"), anyString(), ttl.capture());
        assertThat(ttl.getValue()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void returnsCachedResponseWithoutCallingProvider() {
        OrderService orderService = mock(OrderService.class);
        Kuaidi100Client client = mock(Kuaidi100Client.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order("中通快递", "ZT123456"));
        when(guard.fingerprint(anyString())).thenReturn("cache-fingerprint");

        OrderLogisticsTrackingVO cached = new OrderLogisticsTrackingVO();
        cached.setCompany("中通快递");
        cached.setCompanyCode("zhongtong");
        cached.setExpressNo("ZT123456");
        cached.setCacheExpiresAt(Instant.now().plus(Duration.ofMinutes(20)));
        cached.setTraces(List.of());
        when(guard.getCachedResponse("kuaidi100", "realtime-query", "cache-fingerprint"))
                .thenReturn(JSON.toJSONString(cached));

        OrderLogisticsTrackingVO result = new OrderLogisticsTrackingService(orderService, client, guard)
                .getTracking("SO-001");

        assertThat(result.isCached()).isTrue();
        verify(client, never()).query(anyString(), anyString(), anyString());
    }

    @Test
    void rejectsUnsupportedCourierBeforeCallingProvider() {
        OrderService orderService = mock(OrderService.class);
        Kuaidi100Client client = mock(Kuaidi100Client.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order("自有车队", "CAR-001"));

        assertThatThrownBy(() -> new OrderLogisticsTrackingService(orderService, client, guard)
                .getTracking("SO-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("快递100公司编码");
        verify(client, never()).query(anyString(), anyString(), anyString());
    }

    @Test
    void sendsPhoneOnlyToCouriersThatRequirePhoneValidation() {
        OrderService orderService = mock(OrderService.class);
        Kuaidi100Client client = mock(Kuaidi100Client.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        SalesOrder order = order("韵达快递", "YD123456");
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order);
        when(guard.fingerprint(anyString())).thenReturn("cache-fingerprint");
        when(client.query("yunda", "YD123456", null)).thenReturn(new OrderLogisticsTrackingVO());

        new OrderLogisticsTrackingService(orderService, client, guard).getTracking("SO-001");

        verify(client).query("yunda", "YD123456", null);
    }

    @Test
    void reusesSanitizedFailureCooldownWithoutCallingProviderAgain() {
        OrderService orderService = mock(OrderService.class);
        Kuaidi100Client client = mock(Kuaidi100Client.class);
        ExternalApiGuardService guard = mock(ExternalApiGuardService.class);
        SalesOrder order = order("顺丰速运", "SF123456");
        when(orderService.getSalesOrderForLogisticsTracking("SO-001")).thenReturn(order);
        when(guard.fingerprint(anyString())).thenReturn("cache-fingerprint");
        when(guard.getCachedResponse("kuaidi100", "realtime-query", "cache-fingerprint")).thenReturn(null);
        when(guard.getCachedResponse("kuaidi100", "realtime-query-error", "cache-fingerprint"))
                .thenReturn(null, "{\"code\":502,\"message\":\"快递100查询服务暂时不可用\"}");
        when(client.query("shunfeng", "SF123456", "13800000000"))
                .thenThrow(new BusinessException(502, "快递100查询服务暂时不可用"));
        OrderLogisticsTrackingService service = new OrderLogisticsTrackingService(orderService, client, guard);

        assertThatThrownBy(() -> service.getTracking("SO-001")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getTracking("SO-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("快递100查询服务暂时不可用");
        verify(client, times(1)).query("shunfeng", "SF123456", "13800000000");
    }

    private SalesOrder order(String company, String expressNo) {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-001");
        order.setTenantCode("TENANT_001");
        order.setStatus("shipped");
        order.setCustomerPhone("13800000000");
        order.setExpressCompany(company);
        order.setExpressNo(expressNo);
        return order;
    }
}
