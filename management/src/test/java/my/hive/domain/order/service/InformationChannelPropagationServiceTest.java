package my.hive.domain.order.service;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.domain.print.PrintTaskService;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.installation.mapper.InstallationTaskMapper;
import my.hive.domain.installation.model.entity.InstallationTask;
import my.hive.domain.installation.service.InstallationTaskService;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.SalesOrderDetailMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.OrderFlowPrintTaskRequest;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.entity.ProductionOrder;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.OrderFlowPrintTaskVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformationChannelPropagationServiceTest {

    private static final String INFORMATION_CHANNEL = "Partner referral";

    @Mock
    private CodeGeneratorUtil codeGeneratorUtil;

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private SalesOrderDetailMapper salesOrderDetailMapper;

    @Mock
    private SalesOrderStatusLogMapper salesOrderStatusLogMapper;

    @Mock
    private ProductionOrderMapper productionOrderMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerProjectMapper customerProjectMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private OrderWarningCacheService orderWarningCacheService;

    @Mock
    private OrderNoteService orderNoteService;

    @Mock
    private OrderShipmentService orderShipmentService;

    @Mock
    private PrintTaskService printTaskService;

    @Mock
    private InstallationTaskMapper installationTaskMapper;

    private OrderService subject;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("tenant-a", 1L, Set.of("order:status:budgeting:view", "order:status:budget-completed:view", "order:status:pending-confirm:view", "order:status:pending-pay:view", "order:status:pending-material:view", "order:status:producing:view", "order:status:pending-ship:view", "order:status:shipped:view", "order:status:completed:view", "order:status:pending-cancel:view", "order:status:cancelled:view", "order:status:budgeting:advance", "order:status:budgeting:cancel", "order:status:pending-confirm:advance", "order:status:pending-confirm:cancel", "order:status:pending-pay:advance", "order:status:pending-pay:rollback", "order:status:pending-pay:cancel", "order:status:pending-material:advance", "order:status:pending-material:rollback", "order:status:pending-material:cancel", "order:status:producing:advance", "order:status:producing:rollback", "order:status:producing:cancel", "order:status:pending-ship:advance", "order:status:pending-ship:rollback", "order:status:pending-ship:cancel", "order:status:shipped:advance", "order:status:shipped:rollback", "order:status:shipped:cancel", "order:status:completed:rollback", "order:audit:shipment", "order:audit:cancel"));
        Set<String> tenantPermissions = new java.util.HashSet<>(TenantPermissionContext.getPermCodes());
        tenantPermissions.add("order:scope:tenant");
        TenantPermissionContext.init("tenant-a", 1L, tenantPermissions);
        subject = new OrderService();
        ReflectionTestUtils.setField(subject, "codeGeneratorUtil", codeGeneratorUtil);
        ReflectionTestUtils.setField(subject, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(subject, "salesOrderDetailMapper", salesOrderDetailMapper);
        ReflectionTestUtils.setField(subject, "salesOrderStatusLogMapper", salesOrderStatusLogMapper);
        ReflectionTestUtils.setField(subject, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(subject, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(subject, "customerProjectMapper", customerProjectMapper);
        ReflectionTestUtils.setField(subject, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(subject, "orderWarningCacheService", orderWarningCacheService);
        ReflectionTestUtils.setField(subject, "orderNoteService", orderNoteService);
        ReflectionTestUtils.setField(subject, "orderShipmentService", orderShipmentService);
        ReflectionTestUtils.setField(subject, "printTaskService", printTaskService);
        ReflectionTestUtils.setField(subject, "orderFlowCodeSecret", "test-order-flow-secret");
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void creatingSalesOrderCopiesInformationChannelToProductionOrder() {
        when(codeGeneratorUtil.generateSalesOrderCode()).thenReturn("SO-100");
        when(codeGeneratorUtil.generateProductionOrderCode()).thenReturn("PO-100");
        Customer customer = new Customer();
        customer.setId(10L);
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);

        subject.createSalesOrder(salesRequest());

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(productionOrderMapper).insert(captor.capture());
        ProductionOrder productionOrder = captor.getValue();
        assertEquals("tenant-a", productionOrder.getTenantCode());
        assertEquals("SO-100", productionOrder.getSalesOrderId());
        assertEquals(INFORMATION_CHANNEL, productionOrder.getInformationChannel());
    }

    @Test
    void pendingShipmentOrderCreatesInstallationTask() {
        InstallationTaskService installationTaskService = new InstallationTaskService();
        ReflectionTestUtils.setField(installationTaskService, "installationTaskMapper", installationTaskMapper);
        SalesOrder order = salesOrder("pending_ship");

        installationTaskService.createOrSyncFromInstallationReadyOrder(order);

        ArgumentCaptor<InstallationTask> captor = ArgumentCaptor.forClass(InstallationTask.class);
        verify(installationTaskMapper).insert(captor.capture());
        assertEquals("tenant-a", captor.getValue().getTenantCode());
        assertEquals("SO-100", captor.getValue().getOrderId());
        assertEquals(INFORMATION_CHANNEL, captor.getValue().getInformationChannel());
        assertEquals("production_completed", captor.getValue().getInstallationStatus());
        assertNull(captor.getValue().getOrderCompletedTime());
    }

    @Test
    void producingOrderDoesNotCreateInstallationTask() {
        InstallationTaskService installationTaskService = new InstallationTaskService();
        ReflectionTestUtils.setField(installationTaskService, "installationTaskMapper", installationTaskMapper);

        installationTaskService.createOrSyncFromInstallationReadyOrder(salesOrder("producing"));

        verify(installationTaskMapper, never()).insert(any());
        verify(installationTaskMapper, never()).updateById(any());
    }

    @Test
    void completedOrderPreservesInstallationProgressAndRecordsCompletionTime() {
        InstallationTaskService installationTaskService = new InstallationTaskService();
        ReflectionTestUtils.setField(installationTaskService, "installationTaskMapper", installationTaskMapper);
        InstallationTask existing = new InstallationTask();
        existing.setTenantCode("tenant-a");
        existing.setOrderId("SO-100");
        existing.setInstallationStatus("shipped_pending_install");
        when(installationTaskMapper.selectOne(any())).thenReturn(existing);

        installationTaskService.createOrSyncFromInstallationReadyOrder(salesOrder("completed"));

        ArgumentCaptor<InstallationTask> captor = ArgumentCaptor.forClass(InstallationTask.class);
        verify(installationTaskMapper).updateById(captor.capture());
        assertEquals("shipped_pending_install", captor.getValue().getInstallationStatus());
        assertNotNull(captor.getValue().getOrderCompletedTime());
    }

    @Test
    void salesOrderPrintPayloadContainsInformationChannel() {
        SalesOrder order = salesOrder("pending_confirm");
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(printTaskService.createTask(
                eq("order_flow"), eq("sales_order"), eq("SO-100"), eq("SO-100"),
                any(), isNull(), isNull(), anyString())).thenReturn("PRINT-100");
        OrderFlowPrintTaskRequest request = new OrderFlowPrintTaskRequest();
        request.setOrderId("SO-100");

        OrderFlowPrintTaskVO result = subject.createSalesOrderFlowPrintTask(request);

        Map<String, Object> payload = result.getPrintPayload();
        assertEquals(INFORMATION_CHANNEL, payload.get("informationChannel"));
        assertFalse(payload.containsKey("deliveryDate"));
        assertEquals("PRINT-100", payload.get("printTaskNo"));
    }

    private SalesOrderSaveRequest salesRequest() {
        SalesOrderSaveRequest.ItemDTO item = new SalesOrderSaveRequest.ItemDTO();
        item.setModelCode("MODEL-1");
        item.setQuantity(BigDecimal.ONE);
        item.setWeight("1.2");
        item.setSpec(2.8F);

        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setCustomerName("Customer A");
        request.setProjectName("Project A");
        request.setOrderCategory("bulk");
        request.setInformationChannel(INFORMATION_CHANNEL);
        request.setCreateProductionOrder(1);
        request.setItems(List.of(item));
        return request;
    }

    private SalesOrder salesOrder(String status) {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-100");
        order.setTenantCode("tenant-a");
        order.setStatus(status);
        order.setOrderCategory("bulk");
        order.setCustomerName("Customer A");
        order.setProjectName("Project A");
        order.setInformationChannel(INFORMATION_CHANNEL);
        return order;
    }
}
