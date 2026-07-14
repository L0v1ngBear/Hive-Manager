package my.management.module.order.service;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.print.PrintTaskService;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.entity.Customer;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.installation.mapper.InstallationTaskMapper;
import my.management.module.installation.model.entity.InstallationTask;
import my.management.module.installation.service.InstallationTaskService;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.SalesOrderDetailMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.mapper.SalesOrderStatusLogMapper;
import my.management.module.order.model.dto.OrderFlowPrintTaskRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.vo.OrderFlowPrintTaskVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private PrintTaskService printTaskService;

    @Mock
    private InstallationTaskMapper installationTaskMapper;

    private OrderService subject;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("tenant-a", 1L, Set.of("*"));
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
    void completingSalesOrderCopiesInformationChannelToInstallationTask() {
        InstallationTaskService installationTaskService = new InstallationTaskService();
        ReflectionTestUtils.setField(installationTaskService, "installationTaskMapper", installationTaskMapper);
        SalesOrder order = salesOrder("completed");

        installationTaskService.createOrSyncFromCompletedOrder(order);

        ArgumentCaptor<InstallationTask> captor = ArgumentCaptor.forClass(InstallationTask.class);
        verify(installationTaskMapper).insert(captor.capture());
        assertEquals("tenant-a", captor.getValue().getTenantCode());
        assertEquals("SO-100", captor.getValue().getOrderId());
        assertEquals(INFORMATION_CHANNEL, captor.getValue().getInformationChannel());
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
