package my.hive.domain.order.service;

import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.OrderOperationLogVO;
import my.hive.shared.context.TenantPermissionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderOperationLogQueryContractTest {

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void orderAuditQueryIncludesShipmentOnlyChanges() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderService service = new OrderService();
        ReflectionTestUtils.setField(service, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        TenantPermissionContext.init("TENANT_001", 9L, Set.of(
                "order:scope:tenant", "order:status:pending-ship:view"));

        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-1");
        order.setTenantCode("TENANT_001");
        order.setStatus("pending_ship");
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.<OrderOperationLogVO>of());

        service.listOrderOperationLogs("SO-1", 1, 50);

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> rowSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(countSql.capture(), eq(Long.class), eq("TENANT_001"), eq("SO-1"));
        verify(jdbcTemplate).query(rowSql.capture(), any(RowMapper.class),
                eq("TENANT_001"), eq("SO-1"), eq(50L), eq(0L));
        assertTrue(countSql.getValue().contains("'order_shipment'"));
        assertTrue(rowSql.getValue().contains("'order_shipment'"));
    }
}
