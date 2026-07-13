package my.management.module.order.service;

import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.entity.Customer;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.SalesOrderDetailMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.mapper.SalesOrderStatusLogMapper;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.SalesOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBudgetStateTest {

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
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void drawingBudgetSalesOrderCannotBeCreatedAsBudgetCompleted() {
        when(codeGeneratorUtil.generateSalesOrderCode()).thenReturn("SO-100");
        SalesOrderSaveRequest request = salesRequest("drawing_budget", "budget_completed");

        assertThrows(BusinessException.class, () -> subject.createSalesOrder(request));

        verify(salesOrderMapper, never()).insert(any());
    }

    @Test
    void ordinarySalesOrderCannotFakeBudgetCompletedOnCreate() {
        when(codeGeneratorUtil.generateSalesOrderCode()).thenReturn("SO-100");
        SalesOrderSaveRequest request = salesRequest("bulk", "budget_completed");

        assertThrows(BusinessException.class, () -> subject.createSalesOrder(request));

        verify(salesOrderMapper, never()).insert(any());
    }

    @Test
    void ordinaryProductionOrderCannotFakeBudgetCompletedOnCreate() {
        when(codeGeneratorUtil.generateProductionOrderCode()).thenReturn("PO-100");
        ProductionOrderSaveRequest request = productionRequest("bulk", "budget_completed");

        assertThrows(BusinessException.class, () -> subject.createProductionOrder(request));

        verify(productionOrderMapper, never()).insert(any());
    }

    @Test
    void drawingBudgetMovesFromBudgetingToTerminalBudgetCompleted() {
        when(codeGeneratorUtil.generateSalesOrderCode()).thenReturn("SO-100");
        Customer customer = new Customer();
        customer.setId(10L);
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);

        subject.createSalesOrder(salesRequest("drawing_budget", null));

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderMapper).insert(captor.capture());
        SalesOrder order = captor.getValue();
        assertEquals("budgeting", order.getStatus());
        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(order);

        subject.advanceSalesOrderToNextStage("SO-100", new SalesOrderUpdateRequest());

        assertEquals("budget_completed", order.getStatus());
        assertThrows(BusinessException.class,
                () -> subject.advanceSalesOrderToNextStage("SO-100", new SalesOrderUpdateRequest()));
        verify(salesOrderMapper, times(1)).updateById(order);
    }

    private SalesOrderSaveRequest salesRequest(String category, String status) {
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setCustomerName("Customer A");
        request.setProjectName("Project A");
        request.setOrderCategory(category);
        request.setInformationChannel("drawing_budget".equals(category) ? null : "Direct sales");
        request.setStatus(status);
        return request;
    }

    private ProductionOrderSaveRequest productionRequest(String category, String status) {
        ProductionOrderSaveRequest request = new ProductionOrderSaveRequest();
        request.setOrderCategory(category);
        request.setStatus(status);
        request.setModelCode("MODEL-1");
        request.setWeight(1.2F);
        request.setSpec(2.8F);
        request.setQuantity(1);
        request.setInformationChannel("Direct sales");
        return request;
    }
}
