package my.hive.domain.order.service;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.domain.approval.model.dto.OrderApprovalAuditRequest;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.hive.domain.approval.service.ApprovalService;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.entity.ProductionOrder;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderStatusLog;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingShipApprovalTest {

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private ProductionOrderMapper productionOrderMapper;

    @Mock
    private SalesOrderStatusLogMapper salesOrderStatusLogMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private OrderWarningCacheService orderWarningCacheService;

    @Mock
    private ApprovalAuditorCandidateService approvalAuditorCandidateService;

    @Mock
    private ApprovalDefaultAuditorService approvalDefaultAuditorService;

    @Mock
    private OrderService orderService;

    private OrderService subject;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("tenant-a", 1L, Set.of("order:status:budgeting:view", "order:status:budget-completed:view", "order:status:pending-confirm:view", "order:status:pending-pay:view", "order:status:pending-material:view", "order:status:producing:view", "order:status:pending-ship:view", "order:status:shipped:view", "order:status:completed:view", "order:status:pending-cancel:view", "order:status:cancelled:view", "order:status:budgeting:advance", "order:status:budgeting:cancel", "order:status:pending-confirm:advance", "order:status:pending-confirm:cancel", "order:status:pending-pay:advance", "order:status:pending-pay:rollback", "order:status:pending-pay:cancel", "order:status:pending-material:advance", "order:status:pending-material:rollback", "order:status:pending-material:cancel", "order:status:producing:advance", "order:status:producing:rollback", "order:status:producing:cancel", "order:status:pending-ship:advance", "order:status:pending-ship:rollback", "order:status:pending-ship:cancel", "order:status:shipped:advance", "order:status:shipped:rollback", "order:status:shipped:cancel", "order:status:completed:rollback", "order:audit:shipment", "order:audit:cancel"));
        subject = new OrderService();
        ReflectionTestUtils.setField(subject, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(subject, "salesOrderStatusLogMapper", salesOrderStatusLogMapper);
        ReflectionTestUtils.setField(subject, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(subject, "orderWarningCacheService", orderWarningCacheService);
        ReflectionTestUtils.setField(subject, "approvalAuditorCandidateService", approvalAuditorCandidateService);
        ReflectionTestUtils.setField(subject, "approvalDefaultAuditorService", approvalDefaultAuditorService);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void submittingShipmentApprovalKeepsPendingShipAndPersistsShipmentFields() {
        SalesOrder order = pendingShipOrder();
        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(order);
        when(employeeMapper.selectActiveApproverIdsByPermission(anyString(), anyString())).thenReturn(List.of(2L));
        SalesOrderUpdateRequest request = shipmentRequest();

        subject.advanceSalesOrderToNextStage(order.getOrderId(), request);

        assertEquals("pending_ship", order.getStatus());
        assertEquals("SF Express", order.getExpressCompany());
        assertEquals("SF-100", order.getExpressNo());
        assertEquals("WeChat", order.getInformationChannel());
        assertEquals("ready to ship", order.getRemark());
        verify(salesOrderMapper).updateById(order);
        verify(approvalAuditorCandidateService).replaceActiveCandidates(
                "tenant-a", "ORDER", "sales:SO-100", List.of(2L));
        ArgumentCaptor<SalesOrderStatusLog> logCaptor = ArgumentCaptor.forClass(SalesOrderStatusLog.class);
        verify(salesOrderStatusLogMapper).insert(logCaptor.capture());
        assertEquals("pending_ship", logCaptor.getValue().getOldStatus());
        assertEquals("shipped", logCaptor.getValue().getNewStatus());
        assertEquals("approval_pending", logCaptor.getValue().getOperateType());
    }

    @Test
    void submittingPendingPayApprovalDoesNotPersistUnrelatedBusinessFields() {
        SalesOrder order = pendingPayOrder();
        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(order);
        when(employeeMapper.selectActiveApproverIdsByPermission(anyString(), anyString())).thenReturn(List.of(2L));
        SalesOrderUpdateRequest request = shipmentRequest();
        request.setIsInvoice(1);
        request.setRemark("payment approval submitted");

        subject.advanceSalesOrderToNextStage(order.getOrderId(), request);

        assertEquals("pending_pay", order.getStatus());
        assertEquals("Original channel", order.getInformationChannel());
        assertEquals("Original carrier", order.getExpressCompany());
        assertEquals("OLD-100", order.getExpressNo());
        assertEquals(0, order.getIsInvoice());
        assertEquals("payment approval submitted", order.getRemark());
        verify(salesOrderMapper).updateById(order);
        verify(approvalAuditorCandidateService).replaceActiveCandidates(
                "tenant-a", "ORDER", "sales:SO-100", List.of(2L));
        verify(salesOrderStatusLogMapper, never()).insert(any());
    }

    @Test
    void submittingShipmentApprovalWithoutCompleteLogisticsRejectsWithoutSideEffects() {
        SalesOrder order = pendingShipOrder();
        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(order);
        SalesOrderUpdateRequest request = new SalesOrderUpdateRequest();
        request.setExpressCompany("SF Express");

        assertThrows(BusinessException.class, () -> subject.advanceSalesOrderToNextStage(order.getOrderId(), request));

        assertEquals("pending_ship", order.getStatus());
        verify(salesOrderMapper, never()).updateById(any());
        verify(approvalAuditorCandidateService, never()).replaceActiveCandidates(anyString(), anyString(), anyString(), any());
        verify(salesOrderStatusLogMapper, never()).insert(any());
    }

    @Test
    void submittingShipmentApprovalTwiceRejectsWithoutReplacingActiveCandidates() {
        SalesOrder order = pendingShipOrder();
        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenReturn(order);
        when(approvalAuditorCandidateService.findPendingAuditorIds("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(2L));

        assertThrows(BusinessException.class, () -> subject.advanceSalesOrderToNextStage(order.getOrderId(), shipmentRequest()));

        assertEquals("pending_ship", order.getStatus());
        verify(salesOrderMapper, never()).updateById(any());
        verify(approvalAuditorCandidateService, never()).replaceActiveCandidates(anyString(), anyString(), anyString(), any());
        verify(salesOrderStatusLogMapper, never()).insert(any());
    }

    @Test
    void approvingAllShipmentCandidatesAdvancesOrderToShipped() {
        SalesOrder order = pendingShipOrder();
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED);

        approvalService.auditOrder(orderAuditRequest(1));

        verify(orderService).approveSalesOrderTransition("SO-100", "shipped", "approved");
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void approvingOneOfMultipleShipmentCandidatesKeepsOrderPendingShip() {
        SalesOrder order = pendingShipOrder();
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.PENDING);

        approvalService.auditOrder(orderAuditRequest(1));

        assertEquals("pending_ship", order.getStatus());
        verify(orderService, never()).approveSalesOrderTransition(anyString(), anyString(), anyString());
        verify(approvalAuditorCandidateService, never()).closeActiveCandidates(anyString(), anyString(), anyString());
    }

    @Test
    void rejectingShipmentApprovalKeepsOrderPendingShip() {
        SalesOrder order = pendingShipOrder();
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, false, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.REJECTED);

        approvalService.auditOrder(orderAuditRequest(2));

        assertEquals("pending_ship", order.getStatus());
        verify(orderService, never()).approveSalesOrderTransition(anyString(), anyString(), anyString());
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void rejectingMaterialApprovalKeepsOrderPendingPay() {
        SalesOrder order = pendingPayOrder();
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, false, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.REJECTED);

        approvalService.auditOrder(orderAuditRequest(2));

        assertEquals("pending_pay", order.getStatus());
        verify(orderService, never()).approveSalesOrderTransition(anyString(), anyString(), anyString());
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void lastRollbackApproverAppliesSalesRollbackOnce() {
        SalesOrder order = pendingShipOrder();
        order.setStatus("pending_material");
        when(orderService.hasPendingSalesRollbackApproval("SO-100")).thenReturn(true);
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED);

        approvalService.auditOrder(orderAuditRequest(1));

        verify(orderService).approveSalesOrderRollback("SO-100", "approved");
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void lastRollbackApproverAppliesProductionRollbackOnce() {
        ProductionOrder order = new ProductionOrder();
        order.setOrderId("PO-100");
        order.setTenantCode("tenant-a");
        order.setStatus("pending_material");
        when(orderService.hasPendingProductionRollbackApproval("PO-100")).thenReturn(true);
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "production:PO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED);

        OrderApprovalAuditRequest request = orderAuditRequest(1);
        request.setOrderType("production");
        request.setOrderId("PO-100");
        approvalService.auditOrder(request);

        verify(orderService).approveProductionOrderRollback("PO-100", "approved");
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "production:PO-100");
    }

    @Test
    void completedDrawingBudgetCannotEnterCancellationReview() {
        SalesOrder order = completedDrawingBudgetOrder();
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        SalesOrderUpdateRequest request = new SalesOrderUpdateRequest();
        request.setStatus("pending_cancel");

        assertThrows(BusinessException.class, () -> subject.updateSalesOrder(order.getOrderId(), request));

        assertEquals("budget_completed", order.getStatus());
        verify(salesOrderMapper, never()).updateById(any());
    }

    @Test
    void completedDrawingBudgetCannotSubmitRollbackApproval() {
        SalesOrder order = completedDrawingBudgetOrder();
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        SalesOrderUpdateRequest request = new SalesOrderUpdateRequest();
        request.setAuditorIds(List.of(2L));

        assertThrows(BusinessException.class, () -> subject.submitSalesOrderRollbackApproval(order.getOrderId(), request));

        assertEquals("budget_completed", order.getStatus());
        verify(approvalAuditorCandidateService, never()).replaceActiveCandidates(anyString(), anyString(), anyString(), any());
    }

    @Test
    void legacyPendingCancelDrawingBudgetCannotBeApprovedToCancelled() {
        SalesOrder order = completedDrawingBudgetOrder();
        order.setStatus("pending_cancel");
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED);

        approvalService.auditOrder(orderAuditRequest(1));

        assertEquals("pending_cancel", order.getStatus());
        verify(orderService, never()).approveSalesOrderTransition(anyString(), anyString(), anyString());
        verify(orderService).rejectPendingCancelSalesOrder("SO-100", "approved");
    }

    @Test
    void completedDrawingBudgetApprovalCannotApplyHistoricalRollback() {
        SalesOrder order = completedDrawingBudgetOrder();
        ApprovalService approvalService = approvalServiceFor(order);
        when(approvalAuditorCandidateService.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 1L, true, "approved"))
                .thenReturn(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED);
        when(orderService.hasPendingSalesRollbackApproval("SO-100")).thenReturn(true);

        approvalService.auditOrder(orderAuditRequest(1));

        assertEquals("budget_completed", order.getStatus());
        verify(orderService, never()).approveSalesOrderRollback(anyString(), anyString());
        verify(approvalAuditorCandidateService).closeActiveCandidates("tenant-a", "ORDER", "sales:SO-100");
    }

    private ApprovalService approvalServiceFor(SalesOrder order) {
        ApprovalService approvalService = new ApprovalService();
        ReflectionTestUtils.setField(approvalService, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(approvalService, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(approvalService, "orderService", orderService);
        ReflectionTestUtils.setField(approvalService, "approvalAuditorCandidateService", approvalAuditorCandidateService);
        ReflectionTestUtils.setField(approvalService, "approvalDefaultAuditorService", approvalDefaultAuditorService);
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(approvalAuditorCandidateService.findActiveAuditorIds("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(1L, 2L));
        when(approvalAuditorCandidateService.isPendingAuditor("tenant-a", "ORDER", "sales:SO-100", 1L))
                .thenReturn(true);
        return approvalService;
    }

    private ApprovalService approvalServiceFor(ProductionOrder order) {
        ApprovalService approvalService = new ApprovalService();
        ReflectionTestUtils.setField(approvalService, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(approvalService, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(approvalService, "orderService", orderService);
        ReflectionTestUtils.setField(approvalService, "approvalAuditorCandidateService", approvalAuditorCandidateService);
        ReflectionTestUtils.setField(approvalService, "approvalDefaultAuditorService", approvalDefaultAuditorService);
        when(productionOrderMapper.selectOne(any())).thenReturn(order);
        when(approvalAuditorCandidateService.findActiveAuditorIds("tenant-a", "ORDER", "production:PO-100"))
                .thenReturn(List.of(1L, 2L));
        when(approvalAuditorCandidateService.isPendingAuditor("tenant-a", "ORDER", "production:PO-100", 1L))
                .thenReturn(true);
        return approvalService;
    }

    private SalesOrderUpdateRequest shipmentRequest() {
        SalesOrderUpdateRequest request = new SalesOrderUpdateRequest();
        request.setExpressCompany("SF Express");
        request.setExpressNo("SF-100");
        request.setInformationChannel("WeChat");
        request.setRemark("ready to ship");
        request.setAuditorIds(List.of(2L));
        return request;
    }

    private OrderApprovalAuditRequest orderAuditRequest(int action) {
        OrderApprovalAuditRequest request = new OrderApprovalAuditRequest();
        request.setOrderType("sales");
        request.setOrderId("SO-100");
        request.setAction(action);
        request.setComment("approved");
        return request;
    }

    private SalesOrder pendingShipOrder() {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-100");
        order.setTenantCode("tenant-a");
        order.setOrderCategory("bulk");
        order.setStatus("pending_ship");
        return order;
    }

    private SalesOrder pendingPayOrder() {
        SalesOrder order = pendingShipOrder();
        order.setStatus("pending_pay");
        order.setInformationChannel("Original channel");
        order.setExpressCompany("Original carrier");
        order.setExpressNo("OLD-100");
        order.setIsInvoice(0);
        order.setRemark("original remark");
        return order;
    }

    private SalesOrder completedDrawingBudgetOrder() {
        SalesOrder order = pendingShipOrder();
        order.setOrderCategory("drawing_budget");
        order.setStatus("budget_completed");
        return order;
    }
}
