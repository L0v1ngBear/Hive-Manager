package my.hive.domain.order.service;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.management.module.employee.mapper.EmployeeMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.entity.SalesOrder;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApprovalSubmissionConcurrencyTest {

    @Mock
    private SalesOrderMapper salesOrderMapper;

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

    private OrderService subject;

    @BeforeEach
    void setUp() {
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
    @Timeout(10)
    void concurrentShipmentSubmissionsCreateOnlyOneApprovalWithoutOverwritingWinner() throws Exception {
        SalesOrder order = pendingShipOrder("tenant-a");
        Map<String, List<Long>> activeCandidates = new ConcurrentHashMap<>();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch approvalCreated = new CountDownLatch(1);
        AtomicInteger rowLockCalls = new AtomicInteger();

        when(salesOrderMapper.selectByOrderIdForUpdate("tenant-a", "SO-100")).thenAnswer(invocation -> {
            if (rowLockCalls.incrementAndGet() > 1) {
                assertTrue(approvalCreated.await(5, TimeUnit.SECONDS));
            }
            return order;
        });
        when(employeeMapper.selectActiveApproverIdsByPermission(anyString(), anyString()))
                .thenReturn(List.of(2L, 3L));
        when(approvalAuditorCandidateService.findPendingAuditorIds(
                anyString(), eq("ORDER"), eq("sales:SO-100"))).thenAnswer(invocation -> {
            String key = approvalKey(invocation.getArgument(0));
            return activeCandidates.getOrDefault(key, List.of());
        });
        doAnswer(invocation -> {
            activeCandidates.put(approvalKey(invocation.getArgument(0)), invocation.getArgument(3));
            approvalCreated.countDown();
            return null;
        }).when(approvalAuditorCandidateService).replaceActiveCandidates(
                anyString(), eq("ORDER"), eq("sales:SO-100"), any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> submit("tenant-a", shipmentRequest("First carrier", "FIRST-1", 2L), startGate));
            Future<Throwable> second = executor.submit(() -> submit("tenant-a", shipmentRequest("Second carrier", "SECOND-2", 3L), startGate));
            startGate.countDown();

            Throwable firstFailure = first.get(8, TimeUnit.SECONDS);
            Throwable secondFailure = second.get(8, TimeUnit.SECONDS);

            assertTrue((firstFailure == null) ^ (secondFailure == null));
            assertInstanceOf(BusinessException.class, firstFailure == null ? secondFailure : firstFailure);
            List<Long> winnerCandidates = activeCandidates.get(approvalKey("tenant-a"));
            assertEquals(1, winnerCandidates.size());
            if (winnerCandidates.equals(List.of(2L))) {
                assertEquals("First carrier", order.getExpressCompany());
                assertEquals("FIRST-1", order.getExpressNo());
            } else {
                assertEquals(List.of(3L), winnerCandidates);
                assertEquals("Second carrier", order.getExpressCompany());
                assertEquals("SECOND-2", order.getExpressNo());
            }
            verify(salesOrderMapper, times(1)).updateById(order);
            verify(approvalAuditorCandidateService, times(1)).replaceActiveCandidates(
                    anyString(), eq("ORDER"), eq("sales:SO-100"), any());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sameOrderNumberInDifferentTenantsHasIndependentApprovalSubmission() {
        SalesOrder tenantAOrder = pendingShipOrder("tenant-a");
        SalesOrder tenantBOrder = pendingShipOrder("tenant-b");
        Map<String, SalesOrder> orders = Map.of("tenant-a", tenantAOrder, "tenant-b", tenantBOrder);
        Map<String, List<Long>> activeCandidates = new ConcurrentHashMap<>();

        when(salesOrderMapper.selectByOrderIdForUpdate(anyString(), eq("SO-100")))
                .thenAnswer(invocation -> orders.get(invocation.getArgument(0)));
        when(employeeMapper.selectActiveApproverIdsByPermission(anyString(), anyString()))
                .thenReturn(List.of(2L, 3L));
        when(approvalAuditorCandidateService.findPendingAuditorIds(
                anyString(), eq("ORDER"), eq("sales:SO-100")))
                .thenAnswer(invocation -> activeCandidates.getOrDefault(approvalKey(invocation.getArgument(0)), List.of()));
        doAnswer(invocation -> {
            activeCandidates.put(approvalKey(invocation.getArgument(0)), invocation.getArgument(3));
            return null;
        }).when(approvalAuditorCandidateService).replaceActiveCandidates(
                anyString(), eq("ORDER"), eq("sales:SO-100"), any());

        assertNull(submit("tenant-a", shipmentRequest("A carrier", "A-1", 2L), new CountDownLatch(0)));
        assertNull(submit("tenant-b", shipmentRequest("B carrier", "B-1", 3L), new CountDownLatch(0)));

        assertEquals(List.of(2L), activeCandidates.get(approvalKey("tenant-a")));
        assertEquals(List.of(3L), activeCandidates.get(approvalKey("tenant-b")));
        assertEquals("A-1", tenantAOrder.getExpressNo());
        assertEquals("B-1", tenantBOrder.getExpressNo());
        verify(salesOrderMapper).selectByOrderIdForUpdate("tenant-a", "SO-100");
        verify(salesOrderMapper).selectByOrderIdForUpdate("tenant-b", "SO-100");
    }

    @Test
    void rowLockQueryIsTenantScopedAndUsesDatabaseWriteLock() throws Exception {
        Method method = SalesOrderMapper.class.getMethod(
                "selectByOrderIdForUpdate", String.class, String.class);
        Select select = method.getAnnotation(Select.class);
        InterceptorIgnore interceptorIgnore = method.getAnnotation(InterceptorIgnore.class);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ").toLowerCase();

        assertTrue(sql.contains("tenant_code = #{tenantcode}"));
        assertTrue(sql.contains("order_id = #{orderid}"));
        assertTrue(sql.contains("for update"));
        assertEquals("true", interceptorIgnore.tenantLine());
    }

    private Throwable submit(String tenantCode, SalesOrderUpdateRequest request, CountDownLatch startGate) {
        TenantPermissionContext.init(tenantCode, 1L, Set.of("order:status:budgeting:view", "order:status:budget-completed:view", "order:status:pending-confirm:view", "order:status:pending-pay:view", "order:status:pending-material:view", "order:status:producing:view", "order:status:pending-ship:view", "order:status:shipped:view", "order:status:completed:view", "order:status:pending-cancel:view", "order:status:cancelled:view", "order:status:budgeting:advance", "order:status:budgeting:cancel", "order:status:pending-confirm:advance", "order:status:pending-confirm:cancel", "order:status:pending-pay:advance", "order:status:pending-pay:rollback", "order:status:pending-pay:cancel", "order:status:pending-material:advance", "order:status:pending-material:rollback", "order:status:pending-material:cancel", "order:status:producing:advance", "order:status:producing:rollback", "order:status:producing:cancel", "order:status:pending-ship:advance", "order:status:pending-ship:rollback", "order:status:pending-ship:cancel", "order:status:shipped:advance", "order:status:shipped:rollback", "order:status:shipped:cancel", "order:status:completed:rollback", "order:audit:shipment", "order:audit:cancel"));
        try {
            assertTrue(startGate.await(5, TimeUnit.SECONDS));
            subject.advanceSalesOrderToNextStage("SO-100", request);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        } finally {
            TenantPermissionContext.clear();
        }
    }

    private SalesOrderUpdateRequest shipmentRequest(String carrier, String expressNo, Long auditorId) {
        SalesOrderUpdateRequest request = new SalesOrderUpdateRequest();
        request.setExpressCompany(carrier);
        request.setExpressNo(expressNo);
        request.setInformationChannel("Direct sales");
        request.setRemark(expressNo);
        request.setAuditorIds(List.of(auditorId));
        return request;
    }

    private SalesOrder pendingShipOrder(String tenantCode) {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-100");
        order.setTenantCode(tenantCode);
        order.setOrderCategory("bulk");
        order.setStatus("pending_ship");
        return order;
    }

    private String approvalKey(String tenantCode) {
        return tenantCode + ":ORDER:sales:SO-100";
    }
}
