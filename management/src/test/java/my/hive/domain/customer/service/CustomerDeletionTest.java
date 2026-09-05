package my.hive.domain.customer.service;

import my.hive.domain.aftersales.mapper.AfterSalesTicketMapper;
import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.price.mapper.PriceCustomerOverrideMapper;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerDeletionTest {

    private final CustomerMapper customerMapper = mock(CustomerMapper.class);
    private final CustomerContactMapper customerContactMapper = mock(CustomerContactMapper.class);
    private final CustomerProjectMapper customerProjectMapper = mock(CustomerProjectMapper.class);
    private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
    private final AfterSalesTicketMapper afterSalesTicketMapper = mock(AfterSalesTicketMapper.class);
    private final PriceCustomerOverrideMapper priceCustomerOverrideMapper = mock(PriceCustomerOverrideMapper.class);
    private CustomerService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of());
        service = new CustomerService();
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerContactMapper", customerContactMapper);
        ReflectionTestUtils.setField(service, "customerProjectMapper", customerProjectMapper);
        ReflectionTestUtils.setField(service, "salesOrderMapper", salesOrderMapper);
        ReflectionTestUtils.setField(service, "afterSalesTicketMapper", afterSalesTicketMapper);
        ReflectionTestUtils.setField(service, "priceCustomerOverrideMapper", priceCustomerOverrideMapper);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void deletesOnlyAnUnreferencedCustomerAndItsProfileChildren() {
        when(customerMapper.selectOne(any())).thenReturn(customer());

        service.deleteCustomer(18L);

        verify(salesOrderMapper).selectCount(any());
        verify(afterSalesTicketMapper).selectCount(any());
        verify(priceCustomerOverrideMapper).selectCount(any());
        verify(customerContactMapper).delete(any());
        verify(customerProjectMapper).delete(any());
        verify(customerMapper).delete(any());
    }

    @Test
    void rejectsDeletionWhenTheCustomerHasAfterSalesHistory() {
        when(customerMapper.selectOne(any())).thenReturn(customer());
        when(afterSalesTicketMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteCustomer(18L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("售后工单");

        verify(customerContactMapper, never()).delete(any());
        verify(customerProjectMapper, never()).delete(any());
        verify(customerMapper, never()).delete(any());
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setId(18L);
        customer.setTenantCode("TENANT_001");
        customer.setCustomerName("杭州示例酒店");
        return customer;
    }
}
