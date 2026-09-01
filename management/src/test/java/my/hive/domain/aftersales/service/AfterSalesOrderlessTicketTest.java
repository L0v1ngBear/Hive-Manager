package my.hive.domain.aftersales.service;

import my.hive.domain.aftersales.mapper.AfterSalesTicketMapper;
import my.hive.domain.aftersales.mapper.AfterSalesTicketPartMapper;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketSaveRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.customer.service.CustomerService;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.shared.context.TenantPermissionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
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
    private AfterSalesService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of());
        service = new AfterSalesService();
        ReflectionTestUtils.setField(service, "ticketMapper", ticketMapper);
        ReflectionTestUtils.setField(service, "ticketPartMapper", ticketPartMapper);
        ReflectionTestUtils.setField(service, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(service, "customerService", customerService);
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
        when(customerService.ensureAfterSalesCustomer("杭州新客户", "王经理", "13900001111", "酒店窗帘项目"))
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
        verify(customerService).ensureAfterSalesCustomer("杭州新客户", "王经理", "13900001111", "酒店窗帘项目");
        verify(salesOrderMapper, never()).selectByOrderIdForUpdate(any(), any());
    }
}
