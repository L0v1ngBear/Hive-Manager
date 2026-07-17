package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.order.mapper.SalesOrderShipmentMapper;
import my.hive.domain.order.model.dto.SalesOrderShipmentSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.SalesOrderShipmentVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderShipmentServiceTest {

    private SalesOrderShipmentMapper mapper;
    private EmployeeMapper employeeMapper;
    private OperationLogCollector operationLogCollector;
    private ExternalApiGuardService externalApiGuardService;
    private OrderShipmentService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "order-shipment-test"),
                SalesOrderShipment.class);
        mapper = mock(SalesOrderShipmentMapper.class);
        employeeMapper = mock(EmployeeMapper.class);
        operationLogCollector = mock(OperationLogCollector.class);
        externalApiGuardService = mock(ExternalApiGuardService.class);
        service = new OrderShipmentService(
                mapper, employeeMapper, operationLogCollector, externalApiGuardService);
        TenantPermissionContext.init("TENANT_001", 9L, Set.of());

        Employee employee = new Employee();
        employee.setId(9L);
        employee.setName("Test Operator");
        when(employeeMapper.selectOne(any())).thenReturn(employee);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TenantPermissionContext.clear();
    }

    @Test
    void createsTrimmedShipmentWithTrustedOperator() {
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(existingShipment(101L, 0)));
        when(mapper.insert(any())).thenAnswer(invocation -> {
            invocation.<SalesOrderShipment>getArgument(0).setId(101L);
            return 1;
        });
        when(externalApiGuardService.fingerprint("SF-001")).thenReturn("tracking-fingerprint");
        SalesOrderShipmentSaveRequest request = request(null, null, "  SF Express  ", "  SF-001  ");

        List<SalesOrderShipmentVO> result = service.saveShipments("TENANT_001", "SO-1", List.of(request));

        ArgumentCaptor<SalesOrderShipment> captor = ArgumentCaptor.forClass(SalesOrderShipment.class);
        verify(mapper).insert(captor.capture());
        SalesOrderShipment shipment = captor.getValue();
        assertEquals("SF Express", shipment.getLogisticsCompany());
        assertEquals("SF-001", shipment.getTrackingNo());
        assertEquals(0, shipment.getSortOrder());
        assertEquals("9", shipment.getCreator());
        assertEquals("Test Operator", shipment.getUpdaterName());
        assertEquals(1, result.size());

        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(operationLogCollector).collect(eventCaptor.capture());
        OperationLogEvent event = eventCaptor.getValue();
        assertEquals("TENANT_001", event.getTenantCode());
        assertEquals(9L, event.getUserId());
        assertEquals("add_order_shipment", event.getAction());
        assertEquals("order_shipment", event.getBizType());
        assertEquals("SO-1", event.getBizNo());
        assertEquals("{\"shipmentId\":101,\"trackingFingerprint\":\"tracking-fingerprint\"}", event.getArgsJson());
        assertPersistableShipmentEvent(event);
        assertFalse(event.getArgsJson().contains("SF-001"));
    }

    @Test
    void updatesOnlyChangedShipmentWithVersion() {
        SalesOrderShipment existing = existingShipment(11L, 2);
        when(mapper.selectList(any())).thenReturn(List.of(existing), List.of(existing));
        when(mapper.updateShipment(eq(11L), eq("TENANT_001"), eq("SO-1"), eq(2),
                eq("SF Express"), eq("SF-NEW"), eq(0), any(), any(), any())).thenReturn(1);
        when(externalApiGuardService.fingerprint("SF-NEW")).thenReturn("updated-fingerprint");

        List<SalesOrderShipmentVO> result = service.saveShipments("TENANT_001", "SO-1",
                List.of(request(11L, 2, "SF Express", "SF-NEW")));

        verify(mapper).updateShipment(eq(11L), eq("TENANT_001"), eq("SO-1"), eq(2),
                eq("SF Express"), eq("SF-NEW"), eq(0), any(), any(), any());
        assertEquals(1, result.size());

        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(operationLogCollector).collect(eventCaptor.capture());
        OperationLogEvent event = eventCaptor.getValue();
        assertEquals("update_order_shipment", event.getAction());
        assertEquals("{\"shipmentId\":11,\"trackingFingerprint\":\"updated-fingerprint\"}", event.getArgsJson());
        assertPersistableShipmentEvent(event);
        assertFalse(event.getArgsJson().contains("SF-NEW"));
    }

    @Test
    void leavesUnchangedShipmentUntouched() {
        SalesOrderShipment existing = existingShipment(11L, 2);
        when(mapper.selectList(any())).thenReturn(List.of(existing), List.of(existing));

        service.saveShipments("TENANT_001", "SO-1", List.of(request(11L, 2, "SF Express", "SF-001")));

        verify(mapper, never()).updateShipment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void rejectsOmissionOfPersistedShipment() {
        when(mapper.selectList(any())).thenReturn(List.of(existingShipment(11L, 2)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1", List.of()));

        assertEquals("已保存的物流记录不允许删除", error.getMessage());
        verify(mapper, never()).delete(any());
    }

    @Test
    void rejectsDuplicateTrackingNumbersAfterTrimming() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1", List.of(
                        request(null, null, "SF Express", " SF-001 "),
                        request(null, null, "UPS", "SF-001"))));

        assertEquals("Duplicate tracking number", error.getMessage());
        verify(mapper, never()).insert(any());
    }

    @Test
    void rejectsShipmentOwnedByAnotherOrder() {
        when(mapper.selectList(any())).thenReturn(List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1",
                        List.of(request(11L, 2, "SF Express", "SF-001"))));

        assertEquals("Shipment does not exist or does not belong to this order", error.getMessage());
        verify(mapper, never()).updateShipment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMoreThanFiftyShipments() {
        List<SalesOrderShipmentSaveRequest> requests = java.util.stream.IntStream.range(0, 51)
                .mapToObj(index -> request(null, null, "SF Express", "SF-" + index))
                .toList();

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1", requests));

        assertEquals("At most 50 shipments are allowed", error.getMessage());
        verify(mapper, never()).insert(any());
    }

    @Test
    void updatesWithVersionAndRejectsConcurrentChange() {
        when(mapper.selectList(any())).thenReturn(List.of(existingShipment(11L, 2)));
        when(mapper.updateShipment(eq(11L), eq("TENANT_001"), eq("SO-1"), eq(2),
                eq("SF Express"), eq("SF-NEW"), eq(0), any(), any(), any())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1",
                        List.of(request(11L, 2, "SF Express", "SF-NEW"))));

        assertEquals(409, error.getCode());
        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void emitsNoEventsWhenLaterShipmentUpdateLosesOptimisticLock() {
        SalesOrderShipment first = existingShipment(11L, 2);
        SalesOrderShipment second = existingShipment(12L, 4);
        second.setLogisticsCompany("UPS");
        second.setTrackingNo("UPS-001");
        second.setSortOrder(1);
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        when(mapper.updateShipment(eq(11L), eq("TENANT_001"), eq("SO-1"), eq(2),
                eq("SF Express"), eq("SF-NEW"), eq(0), any(), any(), any())).thenReturn(1);
        when(mapper.updateShipment(eq(12L), eq("TENANT_001"), eq("SO-1"), eq(4),
                eq("UPS"), eq("UPS-NEW"), eq(1), any(), any(), any())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1", List.of(
                        request(11L, 2, "SF Express", "SF-NEW"),
                        request(12L, 4, "UPS", "UPS-NEW"))));

        assertEquals(409, error.getCode());
        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void rejectsZeroRowInsertWithoutEmittingAddEvent() {
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.insert(any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1",
                        List.of(request(null, null, "SF Express", "SF-001"))));

        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void defersSuccessfulShipmentEventsUntilTransactionCommit() {
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(existingShipment(101L, 0)));
        when(mapper.insert(any())).thenAnswer(invocation -> {
            invocation.<SalesOrderShipment>getArgument(0).setId(101L);
            return 1;
        });
        when(externalApiGuardService.fingerprint("SF-001")).thenReturn("tracking-fingerprint");
        TransactionSynchronizationManager.initSynchronization();

        service.saveShipments("TENANT_001", "SO-1",
                List.of(request(null, null, "SF Express", "SF-001")));

        verify(operationLogCollector, never()).collect(any());
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(operationLogCollector).collect(any());
    }

    @Test
    void emitsNoShipmentEventsWhenTransactionRollsBackAfterSuccessfulMethod() {
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(existingShipment(101L, 0)));
        when(mapper.insert(any())).thenAnswer(invocation -> {
            invocation.<SalesOrderShipment>getArgument(0).setId(101L);
            return 1;
        });
        TransactionSynchronizationManager.initSynchronization();

        service.saveShipments("TENANT_001", "SO-1",
                List.of(request(null, null, "SF Express", "SF-001")));
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void listsAndBatchesShipmentsInPersistedSortOrder() {
        SalesOrderShipment first = existingShipment(11L, 1);
        first.setSortOrder(0);
        SalesOrderShipment second = existingShipment(12L, 1);
        second.setOrderId("SO-2");
        second.setSortOrder(0);
        when(mapper.selectList(any())).thenReturn(List.of(first), List.of(first, second));

        List<SalesOrderShipmentVO> listed = service.listShipments("TENANT_001", "SO-1");
        Map<String, List<SalesOrderShipmentVO>> batched = service.listShipmentsByOrderIds(
                "TENANT_001", List.of("SO-1", "SO-2"));

        assertEquals(11L, listed.get(0).getId());
        assertEquals(List.of(11L), batched.get("SO-1").stream().map(SalesOrderShipmentVO::getId).toList());
        assertEquals(List.of(12L), batched.get("SO-2").stream().map(SalesOrderShipmentVO::getId).toList());
    }

    @Test
    void requiresShipmentWithinTenantAndOrder() {
        SalesOrderShipment existing = existingShipment(11L, 2);
        when(mapper.selectOne(any())).thenReturn(existing);

        SalesOrderShipment shipment = service.requireShipment("TENANT_001", "SO-1", 11L);

        assertEquals(11L, shipment.getId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SalesOrderShipment>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectOne(wrapperCaptor.capture());
        LambdaQueryWrapper<SalesOrderShipment> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().matches(
                ".*tenant_code\\s*=.*order_id\\s*=.*id\\s*=.*LIMIT 1.*"));
        assertEquals(Set.<Object>of("TENANT_001", "SO-1", 11L),
                Set.copyOf(wrapper.getParamNameValuePairs().values()));
    }

    @Test
    void acceptsEmptyRequestWhenNoShipmentsExistWithoutWrites() {
        when(mapper.selectList(any())).thenReturn(List.of(), List.of());

        List<SalesOrderShipmentVO> result = service.saveShipments("TENANT_001", "SO-1", List.of());

        assertTrue(result.isEmpty());
        verify(mapper, never()).insert(any());
        verify(mapper, never()).updateShipment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).delete(any());
        verify(operationLogCollector, never()).collect(any());
    }

    @Test
    void rejectsBlankShipmentRows() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveShipments("TENANT_001", "SO-1",
                        List.of(request(null, null, " ", "SF-001"))));

        assertEquals("Logistics company and tracking number are required", error.getMessage());
    }

    private SalesOrderShipmentSaveRequest request(Long id, Integer version, String company, String trackingNo) {
        SalesOrderShipmentSaveRequest request = new SalesOrderShipmentSaveRequest();
        request.setId(id);
        request.setVersion(version);
        request.setLogisticsCompany(company);
        request.setTrackingNo(trackingNo);
        return request;
    }

    private SalesOrderShipment existingShipment(Long id, int version) {
        SalesOrderShipment shipment = new SalesOrderShipment();
        shipment.setId(id);
        shipment.setTenantCode("TENANT_001");
        shipment.setOrderId("SO-1");
        shipment.setLogisticsCompany("SF Express");
        shipment.setTrackingNo("SF-001");
        shipment.setSortOrder(0);
        shipment.setVersion(version);
        shipment.setCreator("8");
        shipment.setUpdater("8");
        shipment.setUpdaterName("Original Operator");
        shipment.setCreateTime(LocalDateTime.now().minusDays(1));
        shipment.setUpdateTime(LocalDateTime.now().minusHours(1));
        return shipment;
    }

    private void assertPersistableShipmentEvent(OperationLogEvent event) {
        assertNotNull(event.getTraceId());
        assertFalse(event.getTraceId().isBlank());
        assertEquals("INFO", event.getLogLevel());
        assertEquals(Boolean.TRUE, event.getSuccess());
        assertEquals(Boolean.FALSE, event.getSlow());
        assertEquals(0L, event.getDurationMs());
        assertNotNull(event.getCreateTime());
    }
}
