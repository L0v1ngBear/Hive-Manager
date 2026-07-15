package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.SalesOrderPageVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceServiceContractTest {

    @Mock
    private SalesOrderMapper salesOrderMapper;

    private OrderService subject;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesOrder.class);
        TenantPermissionContext.init("tenant-a", 1L, Set.of(
                "order:status:budgeting:view", "order:status:budget-completed:view",
                "order:status:pending-confirm:view", "order:status:pending-pay:view",
                "order:status:pending-material:view", "order:status:producing:view",
                "order:status:pending-ship:view", "order:status:shipped:view",
                "order:status:completed:view", "order:status:pending-cancel:view",
                "order:status:cancelled:view"));
        subject = new OrderService();
        ReflectionTestUtils.setField(subject, "salesOrderMapper", salesOrderMapper);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void servicePreservesOtherInvoiceTypeAndRejectsInvalidValues() throws Exception {
        Method normalize = OrderService.class.getDeclaredMethod("normalizeInvoiceFlag", Integer.class);
        normalize.setAccessible(true);

        assertEquals(2, normalize.invoke(subject, 2));
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> normalize.invoke(subject, 3));
        assertEquals(BusinessException.class, exception.getCause().getClass());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void statusSummaryExposesOtherAndSevenDayWarningCounts() {
        when(salesOrderMapper.selectCount(any())).thenReturn(0L);

        Map<String, Long> summary = subject.countSalesOrderStatuses();

        assertEquals(0L, summary.get("invoice_other"));
        assertEquals(0L, summary.get("invoice_warning"));

        ArgumentCaptor<Wrapper<SalesOrder>> wrappers = ArgumentCaptor.forClass(Wrapper.class);
        verify(salesOrderMapper, atLeastOnce()).selectCount(wrappers.capture());
        String warningSql = wrappers.getAllValues().stream()
                .map(Wrapper::getSqlSegment)
                .filter(sql -> sql != null && sql.toUpperCase(Locale.ROOT).contains("CREATE_TIME"))
                .findFirst()
                .orElseThrow();
        String normalizedSql = warningSql.toUpperCase(Locale.ROOT);
        assertTrue(normalizedSql.contains("STATUS IS NULL"));
        assertTrue(normalizedSql.contains("STATUS NOT IN"));
    }

    @Test
    void listContractCarriesIndependentInvoiceWarningFields() {
        SalesOrderPageVO row = new SalesOrderPageVO();
        row.setInvoiceWarning(false);
        row.setInvoiceAgeDays(6L);
        row.setInvoiceWarningDays(7);

        assertFalse(row.getInvoiceWarning());
        assertEquals(6L, row.getInvoiceAgeDays());
        assertEquals(7, row.getInvoiceWarningDays());
    }
}
