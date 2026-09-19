package my.hive.domain.order.service;

import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.infrastructure.logistics.LogisticsTrackingGateway;
import my.hive.infrastructure.logistics.LogisticsTrackingQuery;
import my.hive.shared.external.ExternalApiGuardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import my.hive.shared.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderRecipientTrackingTest {
    @ParameterizedTest
    @CsvSource({"0042,9999,顺丰,0042", "0042,,顺丰,0042", ",9999,顺丰,9999", ",,中通,", ",,顺丰,ERROR"})
    void resolvesSavedSuffixWithoutUsingCustomerPhone(String saved, String supplied, String company, String expected) {
        var orders = mock(OrderService.class);
        var shipments = mock(OrderShipmentService.class);
        var gateway = mock(LogisticsTrackingGateway.class);
        var guard = mock(ExternalApiGuardService.class);
        var order = new SalesOrder();
        order.setOrderId("SO1");
        order.setTenantCode("T1");
        order.setCustomerPhone("13811112222");
        order.setRecipientPhoneSuffix(saved);
        var shipment = new SalesOrderShipment();
        shipment.setId(1L);
        shipment.setOrderId("SO1");
        shipment.setTenantCode("T1");
        shipment.setDeliveryMode("tracked");
        shipment.setLogisticsCompany(company);
        shipment.setTrackingNo("SF123");
        when(orders.getSalesOrderForLogisticsTracking("SO1")).thenReturn(order);
        when(shipments.requireShipment("T1", "SO1", 1L)).thenReturn(shipment);
        when(guard.fingerprint(anyString())).thenAnswer(i -> i.getArgument(0));
        when(gateway.providerCode()).thenReturn("test");
        when(gateway.query(any())).thenReturn(new OrderLogisticsTrackingVO());
        var service = new OrderLogisticsTrackingService(orders, shipments, gateway, guard);
        if ("ERROR".equals(expected)) {
            var error = assertThrows(BusinessException.class, () -> service.getTracking("SO1", 1L, supplied));
            assertEquals("请在订单编辑中补充收件人手机号后4位", error.getMessage());
            verify(gateway, never()).query(any());
        } else {
            service.getTracking("SO1", 1L, supplied);
            verify(gateway).query(new LogisticsTrackingQuery("顺丰".equals(company) ? "SF" : "ZTO", "SF123", expected));
        }
    }

    @Test
    void saveContentPreservesOmittedRecipientAndAllowsExplicitClear() {
        var service = new OrderService();
        var customers = mock(my.hive.domain.customer.mapper.CustomerMapper.class);
        var projects = mock(my.hive.domain.customer.mapper.CustomerProjectMapper.class);
        var customer = new my.hive.domain.customer.model.entity.Customer();
        customer.setId(1L);
        when(customers.selectOne(any())).thenReturn(customer);
        when(projects.selectCount(any())).thenReturn(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "customerMapper", customers);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "customerProjectMapper", projects);
        var request = new my.hive.domain.order.model.dto.SalesOrderSaveRequest();
        request.setCustomerName("测试客户");
        request.setProjectName("测试项目");
        request.setInformationChannel("测试渠道");
        request.setRecipientName(" 收件人甲 ");
        request.setRecipientPhoneSuffix("0042");
        var order = new SalesOrder();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applySalesOrderContent", order, request, true);
        assertEquals("收件人甲", order.getRecipientName());
        assertEquals("0042", order.getRecipientPhoneSuffix());
        var detail = new my.hive.domain.order.model.vo.SalesOrderDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(order, detail);
        assertEquals("0042", detail.getRecipientPhoneSuffix());
        request.setRecipientName(null);
        request.setRecipientPhoneSuffix(null);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applySalesOrderContent", order, request, false);
        assertEquals("收件人甲", order.getRecipientName());
        assertEquals("0042", order.getRecipientPhoneSuffix());
        request.setRecipientName("");
        request.setRecipientPhoneSuffix("");
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applySalesOrderContent", order, request, false);
        assertEquals("", order.getRecipientName());
        assertEquals("", order.getRecipientPhoneSuffix());
    }

    @Test
    void recipientOnlyChangesCountAsOrderContentChanges() {
        var before = new SalesOrder();
        var after = new SalesOrder();
        var service = new OrderService();
        after.setRecipientName("收件人甲");
        assertEquals(true, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "salesOrderContentChanged", before, after));
        after.setRecipientName(null);
        after.setRecipientPhoneSuffix("0042");
        assertEquals(true, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "salesOrderContentChanged", before, after));
    }

    @Test
    void validatesSuffixAsOptionalFourDigitsAndLimitsRecipientName() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var type = my.hive.domain.order.model.dto.SalesOrderSaveRequest.class;
            for (String value : new String[]{null, "", "0042", "9999"}) {
                assertTrue(validator.validateValue(type, "recipientPhoneSuffix", value).isEmpty());
            }
            for (String value : new String[]{"123", "12345", "abcd", "１２３４"}) {
                assertFalse(validator.validateValue(type, "recipientPhoneSuffix", value).isEmpty());
            }
            assertFalse(validator.validateValue(type, "recipientName", "x".repeat(101)).isEmpty());
        }
    }
}
