package my.hive.domain.order;

import my.hive.domain.order.model.enums.OrderStatusEnum;
import my.hive.domain.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnifiedOrderServiceTest {

    @Test
    void canonicalStatusVocabularyRetainsGuardedWorkflowStages() {
        assertEquals("pending_pay", OrderStatusEnum.PENDING_PAY.getCode());
        assertEquals("pending_material", OrderStatusEnum.PENDING_MATERIAL.getCode());
        assertEquals("pending_ship", OrderStatusEnum.PENDING_SHIP.getCode());
        assertEquals("shipped", OrderStatusEnum.SHIPPED.getCode());
    }

    @Test
    void stateTransitionAndRollbackCommandsAreTransactionBoundaries() throws Exception {
        assertRollbackTransaction("advanceSalesOrderToNextStage", String.class,
                my.hive.domain.order.model.dto.SalesOrderUpdateRequest.class);
        assertRollbackTransaction("submitSalesOrderRollbackApproval", String.class,
                my.hive.domain.order.model.dto.SalesOrderUpdateRequest.class);
        assertRollbackTransaction("approveSalesOrderTransition", String.class, String.class, String.class);
        assertRollbackTransaction("approveSalesOrderRollback", String.class, String.class);
    }

    private void assertRollbackTransaction(String name, Class<?>... parameterTypes) throws Exception {
        Method method = OrderService.class.getMethod(name, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, name + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
