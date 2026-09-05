package my.hive.domain.aftersales.service;

import my.hive.domain.aftersales.mapper.AfterSalesTicketMapper;
import my.hive.domain.aftersales.mapper.AfterSalesTicketPartMapper;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketStatusRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.customer.service.CustomerService;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.domain.order.service.OrderLogisticsTrackingService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSalesOrderlessTicketTest {

    private final AfterSalesTicketMapper ticketMapper = mock(AfterSalesTicketMapper.class);
    private final AfterSalesTicketPartMapper ticketPartMapper = mock(AfterSalesTicketPartMapper.class);
    private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
    private final CustomerService customerService = mock(CustomerService.class);
    private final OrderLogisticsTrackingService orderLogisticsTrackingService = mock(OrderLogisticsTrackingService.class);
    private AfterSalesService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of("after_sales:update"));
        service = new AfterSalesService();
        ReflectionTestUtils.setField(service, "ticketMapper", ticketMapper);
        ReflectionTestUtils.setField(service, "ticketPartMapper", ticketPartMapper);
        ReflectionTestUtils.setField(service, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "orderLogisticsTrackingService", orderLogisticsTrackingService);
        ReflectionTestUtils.setField(service, "contextPath", "");
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void createsTicketWithoutOrderAndSynchronizesCustomerCatalog() {
        Customer customer = new Customer();
        customer.setId(88L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州新客户");
        when(customerService.ensureAfterSalesCustomer("杭州新客户", "王经理", "13900001111", "酒店窗帘项目", null, null))
                .thenReturn(customer);

        AtomicReference<AfterSalesTicket> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            AfterSalesTicket ticket = invocation.getArgument(0);
            ticket.setId(99L);
            stored.set(ticket);
            return 1;
        }).when(ticketMapper).insert(any(AfterSalesTicket.class));
        when(ticketMapper.selectOne(any())).thenAnswer(ignored -> stored.get());
        when(ticketPartMapper.selectList(any())).thenReturn(List.of());

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setCustomerName(" 杭州新客户 ");
        request.setProjectName(" 酒店窗帘项目 ");
        request.setContactName("王经理");
        request.setContactPhone("13900001111");
        request.setTicketType("consultation");
        request.setProblemDesc(" 电机偶发离线 ");

        AfterSalesTicket result = service.saveTicket(request);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getOrderId()).isNull();
        assertThat(result.getCustomerName()).isEqualTo("杭州新客户");
        assertThat(result.getProjectName()).isEqualTo("酒店窗帘项目");
        assertThat(result.getProblemDesc()).isEqualTo("电机偶发离线");
        verify(customerService).ensureAfterSalesCustomer("杭州新客户", "王经理", "13900001111", "酒店窗帘项目", null, null);
        verify(salesOrderMapper, never()).selectByOrderIdForUpdate(any(), any());
    }

    @Test
    void createsIntakeTicketWithoutChoosingAProcessingType() {
        Customer customer = new Customer();
        customer.setId(89L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州待指派客户");
        when(customerService.ensureAfterSalesCustomer("杭州待指派客户", "赵经理", "13700001111", "酒店项目", null, null))
                .thenReturn(customer);

        AtomicReference<AfterSalesTicket> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            AfterSalesTicket ticket = invocation.getArgument(0);
            ticket.setId(109L);
            stored.set(ticket);
            return 1;
        }).when(ticketMapper).insert(any(AfterSalesTicket.class));
        when(ticketMapper.selectOne(any())).thenAnswer(ignored -> stored.get());
        when(ticketPartMapper.selectList(any())).thenReturn(List.of());

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setCustomerName("杭州待指派客户");
        request.setProjectName("酒店项目");
        request.setContactName("赵经理");
        request.setContactPhone("13700001111");
        request.setProblemDesc("窗帘无法正常开合");

        AfterSalesTicket result = service.saveTicket(request);

        assertThat(result.getTicketType()).isEqualTo("pending_assignment");
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getDiagnosis()).isNull();
        assertThat(result.getResolution()).isNull();
        verify(ticketPartMapper).delete(any());
    }

    @Test
    void cannotStartIntakeTicketBeforeTheAssigneeConfirmsItsProcessingType() {
        AfterSalesTicket intakeTicket = new AfterSalesTicket();
        intakeTicket.setId(110L);
        intakeTicket.setTenantCode("TENANT_001");
        intakeTicket.setStatus("draft");
        intakeTicket.setTicketType("pending_assignment");
        when(ticketMapper.selectOne(any())).thenReturn(intakeTicket);

        AfterSalesTicketStatusRequest request = new AfterSalesTicketStatusRequest();
        request.setTicketId(110L);
        request.setAction("start");

        assertThatThrownBy(() -> service.updateTicketStatus(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先指派人员并补充处理方式");
    }

    @Test
    void editsProcessingTicketWithoutChangingWorkflowOrReplacingOutboundParts() {
        Customer customer = new Customer();
        customer.setId(88L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州老客户");
        when(customerService.ensureAfterSalesCustomer("杭州老客户", "李经理", "13800001111", "酒店维修项目", null, null))
                .thenReturn(customer);

        AfterSalesTicket processingTicket = new AfterSalesTicket();
        processingTicket.setId(100L);
        processingTicket.setTenantCode("TENANT_001");
        processingTicket.setTicketNo("AS202609010001");
        processingTicket.setStatus("processing");
        processingTicket.setApprovalRequired(1);
        processingTicket.setProblemDesc("原问题描述");
        when(ticketMapper.selectOne(any())).thenReturn(processingTicket);
        when(ticketPartMapper.selectList(any())).thenReturn(List.of());

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setId(100L);
        request.setCustomerName("杭州老客户");
        request.setProjectName("酒店维修项目");
        request.setContactName("李经理");
        request.setContactPhone("13800001111");
        request.setTicketType("on_site_repair");
        request.setProblemDesc("修订后的问题描述");
        request.setResolution("补充维修结果");
        request.setApprovalRequired(false);

        AfterSalesTicket result = service.saveTicket(request);

        assertThat(result.getStatus()).isEqualTo("processing");
        assertThat(result.getApprovalRequired()).isEqualTo(1);
        assertThat(result.getProblemDesc()).isEqualTo("修订后的问题描述");
        assertThat(result.getResolution()).isEqualTo("补充维修结果");
        verify(ticketMapper).updateById(processingTicket);
        verify(ticketPartMapper, never()).delete(any());
        verify(ticketPartMapper, never()).insert(any());
    }

    @Test
    void assignedProcessorCanAddOnSiteServiceDetailsWithoutChangingWorkflow() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of("after_sales:process"));
        Customer customer = new Customer();
        customer.setId(88L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州老客户");
        when(customerService.ensureAfterSalesCustomer("杭州老客户", "李经理", "13800001111", "酒店维修项目", null, null))
                .thenReturn(customer);

        AfterSalesTicket processingTicket = new AfterSalesTicket();
        processingTicket.setId(101L);
        processingTicket.setTenantCode("TENANT_001");
        processingTicket.setTicketNo("AS202609010002");
        processingTicket.setStatus("processing");
        processingTicket.setAssigneeUserId(7L);
        processingTicket.setApprovalRequired(1);
        when(ticketMapper.selectOne(any())).thenReturn(processingTicket);
        when(ticketPartMapper.selectList(any())).thenReturn(List.of());

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setId(101L);
        request.setCustomerName("杭州老客户");
        request.setProjectName("酒店维修项目");
        request.setContactName("李经理");
        request.setContactPhone("13800001111");
        request.setTicketType("on_site_repair");
        request.setProblemDesc("现场维修");
        request.setScheduledTime(LocalDateTime.of(2026, 9, 4, 10, 30));
        request.setTechnicianName("王经理");
        request.setRepairAmount(new BigDecimal("680.00"));

        AfterSalesTicket result = service.saveTicket(request);

        assertThat(result.getStatus()).isEqualTo("processing");
        assertThat(result.getApprovalRequired()).isEqualTo(1);
        assertThat(result.getScheduledTime()).isEqualTo(LocalDateTime.of(2026, 9, 4, 10, 30));
        assertThat(result.getTechnicianName()).isEqualTo("王经理");
        assertThat(result.getRepairAmount()).isEqualByComparingTo("680.00");
        verify(ticketPartMapper, never()).delete(any());
        verify(ticketPartMapper, never()).insert(any());
    }

    @Test
    void savesMotorReturnDirectionsAndRegistrationDateIndependently() {
        Customer customer = new Customer();
        customer.setId(88L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州老客户");
        when(customerService.ensureAfterSalesCustomer("杭州老客户", "李经理", "13800001111", "酒店维修项目", null, null))
                .thenReturn(customer);

        AfterSalesTicket draftTicket = new AfterSalesTicket();
        draftTicket.setId(103L);
        draftTicket.setTenantCode("TENANT_001");
        draftTicket.setTicketNo("AS202609010003");
        draftTicket.setStatus("draft");
        when(ticketMapper.selectOne(any())).thenReturn(draftTicket);
        when(ticketPartMapper.selectList(any())).thenReturn(List.of());

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setId(103L);
        request.setCustomerName("杭州老客户");
        request.setProjectName("酒店维修项目");
        request.setContactName("李经理");
        request.setContactPhone("13800001111");
        request.setTicketType("motor_replacement");
        request.setProblemDesc("电机返厂检测");
        request.setRegistrationDate(LocalDate.of(2026, 9, 4));
        request.setLogisticsCompany("顺丰速运");
        request.setWaybillNo("CUSTOMER-TO-FACTORY-001");
        request.setManufacturerReturnLogisticsCompany("京东快递");
        request.setManufacturerReturnWaybillNo("FACTORY-TO-CUSTOMER-001");

        AfterSalesTicket result = service.saveTicket(request);

        assertThat(result.getRegistrationDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(result.getLogisticsCompany()).isEqualTo("顺丰速运");
        assertThat(result.getWaybillNo()).isEqualTo("CUSTOMER-TO-FACTORY-001");
        assertThat(result.getManufacturerReturnLogisticsCompany()).isEqualTo("京东快递");
        assertThat(result.getManufacturerReturnWaybillNo()).isEqualTo("FACTORY-TO-CUSTOMER-001");
    }

    @Test
    void processorCannotEditTicketAssignedToAnotherEmployee() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of("after_sales:process"));
        AfterSalesTicket processingTicket = new AfterSalesTicket();
        processingTicket.setId(102L);
        processingTicket.setTenantCode("TENANT_001");
        processingTicket.setStatus("processing");
        processingTicket.setAssigneeUserId(8L);
        when(ticketMapper.selectOne(any())).thenReturn(processingTicket);

        AfterSalesTicketSaveRequest request = new AfterSalesTicketSaveRequest();
        request.setId(102L);
        request.setTicketType("on_site_repair");
        request.setProblemDesc("现场维修");

        assertThatThrownBy(() -> service.saveTicket(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅被指派人");
        verify(customerService, never()).ensureAfterSalesCustomer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void tracksAnOrderlessAfterSalesWaybillUsingTheTicketTenant() {
        AfterSalesTicket ticket = new AfterSalesTicket();
        ticket.setId(201L);
        ticket.setTenantCode("TENANT_001");
        ticket.setLogisticsCompany("德邦快递");
        ticket.setWaybillNo("DPK212715394227");
        when(ticketMapper.selectOne(any())).thenReturn(ticket);
        OrderLogisticsTrackingVO tracking = new OrderLogisticsTrackingVO();
        tracking.setTrackingNo("DPK212715394227");
        when(orderLogisticsTrackingService.getTrackingForAfterSales("TENANT_001", "德邦快递", "DPK212715394227", 201L))
                .thenReturn(tracking);

        OrderLogisticsTrackingVO result = service.ticketLogisticsTracking(201L);

        assertThat(result).isSameAs(tracking);
        verify(orderLogisticsTrackingService)
                .getTrackingForAfterSales("TENANT_001", "德邦快递", "DPK212715394227", 201L);
    }

    @Test
    void tellsOrderlessTicketsToFillTheCourierInsteadOfRequiringAnOrder() {
        AfterSalesTicket ticket = new AfterSalesTicket();
        ticket.setId(202L);
        ticket.setTenantCode("TENANT_001");
        ticket.setWaybillNo("DPK212715394228");
        when(ticketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> service.ticketLogisticsTracking(202L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该售后工单缺少物流公司，请编辑工单后补充");
    }
}
