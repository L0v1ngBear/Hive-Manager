package my.hive.domain.order.service;

import my.hive.domain.order.model.enums.OrderInvoiceStatusEnum;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderInvoiceWarningPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 0);

    @Test
    void invoiceStatusAcceptsOnlyThreeCommercialValues() {
        assertEquals(0, OrderInvoiceStatusEnum.normalize(null));
        assertEquals(0, OrderInvoiceStatusEnum.normalize(0));
        assertEquals(1, OrderInvoiceStatusEnum.normalize(1));
        assertEquals(2, OrderInvoiceStatusEnum.normalize(2));
        assertThrows(BusinessException.class, () -> OrderInvoiceStatusEnum.normalize(3));
        assertThrows(BusinessException.class, () -> OrderInvoiceStatusEnum.normalize(-1));
    }

    @Test
    void unissuedOrderWarnsAtSevenCompleteDays() {
        OrderInvoiceWarningPolicy.WarningResult sixDays = OrderInvoiceWarningPolicy.evaluate(
                0, "completed", NOW.minusDays(6), NOW);
        OrderInvoiceWarningPolicy.WarningResult sevenDays = OrderInvoiceWarningPolicy.evaluate(
                0, "completed", NOW.minusDays(7), NOW);

        assertFalse(sixDays.warning());
        assertEquals(6L, sixDays.ageDays());
        assertTrue(sevenDays.warning());
        assertEquals(7L, sevenDays.ageDays());
        assertEquals(7, sevenDays.warningDays());
    }

    @Test
    void settledAndCancelledOrdersNeverWarn() {
        assertFalse(OrderInvoiceWarningPolicy.evaluate(1, "completed", NOW.minusDays(30), NOW).warning());
        assertFalse(OrderInvoiceWarningPolicy.evaluate(2, "completed", NOW.minusDays(30), NOW).warning());
        assertFalse(OrderInvoiceWarningPolicy.evaluate(0, "pending_cancel", NOW.minusDays(30), NOW).warning());
        assertFalse(OrderInvoiceWarningPolicy.evaluate(0, "cancelled", NOW.minusDays(30), NOW).warning());
    }

    @Test
    void warningAgeUsesCreateTimeOnly() {
        OrderInvoiceWarningPolicy.WarningResult result = OrderInvoiceWarningPolicy.evaluate(
                0, "producing", NOW.minusDays(12), NOW);

        assertTrue(result.warning());
        assertEquals(12L, result.ageDays());
    }
}
