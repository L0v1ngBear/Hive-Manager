package my.hive.domain.order.service;

import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.ProductionOrderStatusLogMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.dto.ProductionOrderUpdateRequest;
import my.hive.domain.order.model.entity.ProductionOrder;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedOrderProcessServiceTest {

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private ProductionOrderMapper productionOrderMapper;

    @Mock
    private ProductionOrderStatusLogMapper productionOrderStatusLogMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private OrderWarningCacheService orderWarningCacheService;

    private OrderService subject;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("tenant-a", 1L, Set.of(
                "order:status:producing:view",
                "order:status:producing:advance"
        ));
        subject = new OrderService();
        ReflectionTestUtils.setField(subject, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(subject, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(subject, "productionOrderStatusLogMapper", productionOrderStatusLogMapper);
        ReflectionTestUtils.setField(subject, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(subject, "orderWarningCacheService", orderWarningCacheService);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void unifiedProcessUpdateAdvancesLaggingTasksWithoutRollingBackTasksAlreadyAhead() {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setOrderId("SO-100");
        salesOrder.setTenantCode("tenant-a");

        ProductionOrder lagging = productionOrder("PO-1", 1);
        ProductionOrder ahead = productionOrder("PO-2", 3);

        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(salesOrder);
        when(productionOrderMapper.selectList(any())).thenReturn(List.of(lagging, ahead));
        when(productionOrderMapper.selectOne(any())).thenReturn(lagging, ahead);

        ProductionOrderUpdateRequest request = new ProductionOrderUpdateRequest();
        request.setProcess(2);

        subject.updateSalesOrderProcess("SO-100", request);

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(productionOrderMapper, times(2)).updateById(captor.capture());
        assertEquals(List.of(2, 3), captor.getAllValues().stream().map(ProductionOrder::getProcess).toList());
    }

    @Test
    void unifiedProcessUpdateRejectsEmptyRequestsBeforeLockingTheOrder() {
        assertThrows(BusinessException.class, () -> subject.updateSalesOrderProcess("SO-100", null));

        verify(salesOrderMapper, never()).selectByOrderIdForUpdate(any(), any());
    }

    @Test
    void unifiedProcessUpdateCannotShipTheOrder() {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setOrderId("SO-100");
        salesOrder.setTenantCode("tenant-a");
        ProductionOrder productionOrder = productionOrder("PO-1", 9);

        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(salesOrder);
        when(productionOrderMapper.selectList(any())).thenReturn(List.of(productionOrder));

        ProductionOrderUpdateRequest request = new ProductionOrderUpdateRequest();
        request.setStatus("shipped");

        assertThrows(BusinessException.class, () -> subject.updateSalesOrderProcess("SO-100", request));

        verify(productionOrderMapper, never()).updateById(any());
    }

    private ProductionOrder productionOrder(String orderId, int process) {
        ProductionOrder order = new ProductionOrder();
        order.setOrderId(orderId);
        order.setSalesOrderId("SO-100");
        order.setTenantCode("tenant-a");
        order.setStatus("producing");
        order.setProcess(process);
        return order;
    }
}
