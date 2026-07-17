package my.hive.domain.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.installation.service.InstallationTaskService;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.SalesOrderDetailMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.SalesOrderPageRequest;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderShipmentSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderStatusLog;
import my.hive.domain.order.model.vo.SalesOrderDetailVO;
import my.hive.domain.order.model.vo.SalesOrderPageVO;
import my.hive.domain.order.model.vo.SalesOrderShipmentVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.utils.CodeGeneratorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMultiShipmentLifecycleTest {

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
    private CustomerContactMapper customerContactMapper;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private CodeGeneratorUtil codeGeneratorUtil;
    @Mock
    private OrderWarningCacheService orderWarningCacheService;
    @Mock
    private OrderSettingService orderSettingService;
    @Mock
    private OrderNoteService orderNoteService;
    @Mock
    private OrderShipmentService orderShipmentService;
    @Mock
    private InstallationTaskService installationTaskService;
    @Mock
    private ApprovalAuditorCandidateService approvalAuditorCandidateService;

    private OrderService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 1L, Set.of(
                "order:scope:tenant",
                "order:status:pending-confirm:view",
                "order:status:pending-ship:view",
                "order:status:shipped:view",
                "order:status:completed:view",
                "order:status:pending-ship:advance",
                "order:audit:shipment"
        ));
        service = new OrderService();
        ReflectionTestUtils.setField(service, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(service, "salesOrderDetailMapper", salesOrderDetailMapper);
        ReflectionTestUtils.setField(service, "salesOrderStatusLogMapper", salesOrderStatusLogMapper);
        ReflectionTestUtils.setField(service, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerProjectMapper", customerProjectMapper);
        ReflectionTestUtils.setField(service, "customerContactMapper", customerContactMapper);
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "codeGeneratorUtil", codeGeneratorUtil);
        ReflectionTestUtils.setField(service, "orderWarningCacheService", orderWarningCacheService);
        ReflectionTestUtils.setField(service, "orderSettingService", orderSettingService);
        ReflectionTestUtils.setField(service, "orderNoteService", orderNoteService);
        ReflectionTestUtils.setField(service, "orderShipmentService", orderShipmentService);
        ReflectionTestUtils.setField(service, "installationTaskService", installationTaskService);
        ReflectionTestUtils.setField(service, "approvalAuditorCandidateService", approvalAuditorCandidateService);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void orderContractsExposeShipmentListsWithoutScalarLogistics() throws Exception {
        Field shipments = SalesOrderSaveRequest.class.getDeclaredField("shipments");
        assertThat(shipments.getType()).isEqualTo(List.class);
        assertThat(shipments.getAnnotation(Valid.class)).isNotNull();
        assertThat(shipments.getAnnotation(Size.class).max()).isEqualTo(50);

        assertThat(fieldNames(SalesOrder.class)).doesNotContain("expressCompany", "expressNo");
        assertThat(fieldNames(SalesOrderUpdateRequest.class)).doesNotContain("expressCompany", "expressNo", "shipments");
        assertThat(fieldNames(SalesOrderPageVO.class)).contains("shipments");
        assertThat(fieldNames(SalesOrderDetailVO.class)).contains("shipments");
    }

    @Test
    void pageProjectsShipmentsWithOneBatchLookup() {
        SalesOrder first = order("SO-1", "pending_confirm");
        SalesOrder second = order("SO-2", "pending_ship");
        Page<SalesOrder> source = new Page<>(1, 10, 2);
        source.setRecords(List.of(first, second));
        SalesOrderShipmentVO shipment = shipment("SF-100");
        when(salesOrderMapper.selectPage(any(Page.class), any())).thenReturn(source);
        when(salesOrderDetailMapper.selectList(any())).thenReturn(List.of());
        when(productionOrderMapper.selectList(any())).thenReturn(List.of());
        when(orderShipmentService.listShipmentsByOrderIds("TENANT_001", List.of("SO-1", "SO-2")))
                .thenReturn(Map.of("SO-1", List.of(shipment), "SO-2", List.of()));

        Page<SalesOrderPageVO> result = service.pageSalesOrders(new SalesOrderPageRequest());

        assertThat(result.getRecords()).extracting(SalesOrderPageVO::getShipments)
                .containsExactly(List.of(shipment), List.of());
        verify(orderShipmentService).listShipmentsByOrderIds("TENANT_001", List.of("SO-1", "SO-2"));
    }

    @Test
    void detailProjectsPersistedShipments() {
        SalesOrder order = order("SO-1", "pending_confirm");
        SalesOrderShipmentVO shipment = shipment("YT-100");
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(salesOrderDetailMapper.selectList(any())).thenReturn(List.of());
        when(productionOrderMapper.selectList(any())).thenReturn(List.of());
        when(orderNoteService.listNotesIfPermitted("TENANT_001", "SO-1")).thenReturn(List.of());
        when(salesOrderStatusLogMapper.selectList(any())).thenReturn(List.of());
        when(orderShipmentService.listShipments("TENANT_001", "SO-1")).thenReturn(List.of(shipment));

        SalesOrderDetailVO result = service.getSalesOrderDetail("SO-1");

        assertThat(result.getShipments()).containsExactly(shipment);
    }

    @Test
    void createPersistsFullShipmentList() {
        prepareCreateDependencies();
        SalesOrderSaveRequest request = validRequest();
        List<SalesOrderShipmentSaveRequest> shipments = List.of(shipmentRequest("SF-100"));
        request.setShipments(shipments);
        when(orderShipmentService.saveShipments("TENANT_001", "SO-100", shipments))
                .thenReturn(List.of(shipment("SF-100")));

        assertThat(service.createSalesOrder(request)).isEqualTo("SO-100");

        verify(salesOrderMapper).insert(any(SalesOrder.class));
        verify(orderShipmentService).saveShipments("TENANT_001", "SO-100", shipments);
    }

    @Test
    void completeSavePersistsFullShipmentList() {
        SalesOrder existing = order("SO-100", "pending_confirm");
        existing.setCustomerName("Customer");
        existing.setProjectName("Project");
        existing.setOrderCategory("bulk");
        existing.setInformationChannel("WeChat");
        existing.setIsInvoice(0);
        existing.setCreateTime(LocalDateTime.now());
        existing.setUpdateTime(existing.getCreateTime());
        SalesOrderSaveRequest request = validRequest();
        List<SalesOrderShipmentSaveRequest> shipments = List.of(shipmentRequest("ZT-100"));
        request.setShipments(shipments);
        Customer customer = new Customer();
        customer.setId(1L);
        when(salesOrderMapper.selectOne(any())).thenReturn(existing);
        when(salesOrderDetailMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);
        when(orderShipmentService.saveShipments("TENANT_001", "SO-100", shipments))
                .thenReturn(List.of(shipment("ZT-100")));

        service.saveSalesOrder("SO-100", request);

        verify(orderShipmentService).saveShipments("TENANT_001", "SO-100", shipments);
    }

    @Test
    void shippedOrderRequiresAtLeastOneShipment() {
        prepareCreateDependencies();
        SalesOrderSaveRequest request = validRequest();
        request.setStatus("shipped");
        request.setShipments(List.of());
        when(orderShipmentService.saveShipments("TENANT_001", "SO-100", List.of())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createSalesOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少填写一条物流信息");

        verify(orderShipmentService).saveShipments("TENANT_001", "SO-100", List.of());
        verify(installationTaskService, never()).createOrSyncFromCompletedOrder(any());
    }

    @Test
    void advanceValidatesOnlyPersistedShipments() {
        SalesOrder order = order("SO-100", "pending_ship");
        when(salesOrderMapper.selectByOrderIdForUpdate("TENANT_001", "SO-100")).thenReturn(order);
        when(orderShipmentService.listShipments("TENANT_001", "SO-100")).thenReturn(List.of());

        assertThatThrownBy(() -> service.advanceSalesOrderToNextStage("SO-100", new SalesOrderUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少填写一条物流信息");

        verify(orderShipmentService).listShipments("TENANT_001", "SO-100");
        verify(salesOrderMapper, never()).updateById(any());
    }

    @Test
    void approvedRollbackToShippedRejectsEmptyPersistedShipmentsBeforeWrite() {
        SalesOrder order = order("SO-100", "completed");
        SalesOrderStatusLog rollbackLog = statusLog("completed", "shipped");
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(salesOrderStatusLogMapper.selectOne(any())).thenReturn(rollbackLog);
        when(orderShipmentService.listShipments("TENANT_001", "SO-100")).thenReturn(List.of());

        assertThatThrownBy(() -> service.approveSalesOrderRollback("SO-100", "approved"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("\u81f3\u5c11\u586b\u5199\u4e00\u6761\u7269\u6d41\u4fe1\u606f");

        verify(orderShipmentService).listShipments("TENANT_001", "SO-100");
        verify(salesOrderMapper, never()).updateById(any());
    }

    @Test
    void rejectedCancellationRestoringShippedRejectsEmptyPersistedShipmentsBeforeWrite() {
        SalesOrder order = order("SO-100", "pending_cancel");
        SalesOrderStatusLog cancelLog = statusLog("shipped", "pending_cancel");
        when(salesOrderMapper.selectOne(any())).thenReturn(order);
        when(salesOrderStatusLogMapper.selectOne(any())).thenReturn(cancelLog);
        when(orderShipmentService.listShipments("TENANT_001", "SO-100")).thenReturn(List.of());

        assertThatThrownBy(() -> service.rejectPendingCancelSalesOrder("SO-100", "rejected"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("\u81f3\u5c11\u586b\u5199\u4e00\u6761\u7269\u6d41\u4fe1\u606f");

        verify(orderShipmentService).listShipments("TENANT_001", "SO-100");
        verify(salesOrderMapper, never()).updateById(any());
    }

    @Test
    void orderWritesAreRollbackTransactionsAndInstallationSyncIsIndependent() throws Exception {
        assertRollbackTransaction("createSalesOrder", SalesOrderSaveRequest.class);
        assertRollbackTransaction("saveSalesOrder", String.class, SalesOrderSaveRequest.class);
        assertRollbackTransaction("advanceSalesOrderToNextStage", String.class, SalesOrderUpdateRequest.class);

        String orderSource = readSource("my/hive/domain/order/service/OrderService.java")
                + readSource("my/hive/domain/order/model/entity/SalesOrder.java")
                + readSource("my/hive/domain/order/model/dto/SalesOrderSaveRequest.java")
                + readSource("my/hive/domain/order/model/dto/SalesOrderUpdateRequest.java")
                + readSource("my/hive/domain/order/model/vo/SalesOrderPageVO.java")
                + readSource("my/hive/domain/order/model/vo/SalesOrderDetailVO.java");
        assertThat(orderSource).doesNotContain(
                "getExpressCompany", "setExpressCompany", "getExpressNo", "setExpressNo");

        String installationSource = readSource("my/hive/domain/installation/service/InstallationTaskService.java");
        int copyStart = installationSource.indexOf("private void copyOrderFields");
        int copyEnd = installationSource.indexOf("private InstallationTaskVO toVO", copyStart);
        assertThat(installationSource.substring(copyStart, copyEnd)).doesNotContain(
                "order.getExpressCompany", "order.getExpressNo", "getShipments");
        assertThat(installationSource).contains(
                "request.getExpressCompany()", "request.getExpressNo()");
    }

    private void prepareCreateDependencies() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(codeGeneratorUtil.generateSalesOrderCode()).thenReturn("SO-100");
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);
    }

    private SalesOrderSaveRequest validRequest() {
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setCustomerName("Customer");
        request.setProjectName("Project");
        request.setOrderCategory("bulk");
        request.setInformationChannel("WeChat");
        request.setStatus("pending_confirm");
        request.setItems(List.of());
        request.setNotes(List.of());
        request.setShipments(List.of());
        return request;
    }

    private SalesOrder order(String orderId, String status) {
        SalesOrder order = new SalesOrder();
        order.setOrderId(orderId);
        order.setTenantCode("TENANT_001");
        order.setStatus(status);
        order.setOrderCategory("bulk");
        return order;
    }

    private SalesOrderStatusLog statusLog(String oldStatus, String newStatus) {
        SalesOrderStatusLog log = new SalesOrderStatusLog();
        log.setOrderId("SO-100");
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        return log;
    }

    private SalesOrderShipmentSaveRequest shipmentRequest(String trackingNo) {
        SalesOrderShipmentSaveRequest request = new SalesOrderShipmentSaveRequest();
        request.setLogisticsCompany("Carrier");
        request.setTrackingNo(trackingNo);
        return request;
    }

    private SalesOrderShipmentVO shipment(String trackingNo) {
        SalesOrderShipmentVO shipment = new SalesOrderShipmentVO();
        shipment.setLogisticsCompany("Carrier");
        shipment.setTrackingNo(trackingNo);
        return shipment;
    }

    private List<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).toList();
    }

    private void assertRollbackTransaction(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = OrderService.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }

    private String readSource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java").resolve(relativePath));
    }
}
