package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDataScopeFilterTest {

    private final OrderService service = new OrderService();

    @AfterEach
    void clearContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void salesSelfScopeFiltersOrdersByCurrentCreator() {
        TenantPermissionContext.init("TENANT_001", 27L, Set.of("order:scope:sales:self"));

        LambdaQueryWrapper<SalesOrder> wrapper = scopedWrapper();

        assertThat(wrapper.getCustomSqlSegment()).contains("creator");
        assertThat(wrapper.getParamNameValuePairs()).containsValue("27");
    }

    @Test
    void tenantScopeDoesNotAddAUserFilter() {
        TenantPermissionContext.init("TENANT_001", 27L, Set.of("order:scope:tenant"));

        LambdaQueryWrapper<SalesOrder> wrapper = scopedWrapper();

        assertThat(wrapper.getCustomSqlSegment()).isBlank();
    }

    @Test
    void missingOrderScopeFailsClosed() {
        TenantPermissionContext.init("TENANT_001", 27L, Set.of("order:list"));

        LambdaQueryWrapper<SalesOrder> wrapper = scopedWrapper();

        assertThat(wrapper.getCustomSqlSegment()).contains("1 = 0");
    }

    @Test
    void directOrderAccessUsesTheSameSalesOwnershipScope() {
        SalesOrder ownOrder = order("27");
        TenantPermissionContext.init("TENANT_001", 27L, Set.of("order:scope:sales:self"));

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "assertSalesOrderDataScope", ownOrder))
                .doesNotThrowAnyException();

        SalesOrder anotherUsersOrder = order("28");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "assertSalesOrderDataScope", anotherUsersOrder))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(403);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<SalesOrder> scopedWrapper() {
        return (LambdaQueryWrapper<SalesOrder>) ReflectionTestUtils.invokeMethod(
                service, "scopedSalesOrderWrapper", new Object[]{null});
    }

    private SalesOrder order(String creator) {
        SalesOrder order = new SalesOrder();
        order.setOrderId("SO-1");
        order.setTenantCode("TENANT_001");
        order.setCreator(creator);
        return order;
    }
}
