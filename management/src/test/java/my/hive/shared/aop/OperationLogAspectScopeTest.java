package my.hive.shared.aop;

import my.hive.shared.annotation.CollectLog;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperationLogAspectScopeTest {

    @Test
    void recordsOrderOperationsButSkipsOtherModules() throws Throwable {
        OperationLogProperties properties = new OperationLogProperties();
        properties.setRecordedModules(Set.of("order"));
        OperationLogCollector collector = mock(OperationLogCollector.class);
        SensitiveDataSanitizer sanitizer = mock(SensitiveDataSanitizer.class);
        OperationLogAspect aspect = new OperationLogAspect(properties, collector, sanitizer);

        ProceedingJoinPoint customerJoinPoint = mock(ProceedingJoinPoint.class);
        when(customerJoinPoint.proceed()).thenReturn("customer-result");
        Object customerResult = aspect.around(customerJoinPoint, annotation("customerOperation"));
        assertThat(customerResult).isEqualTo("customer-result");
        verifyNoInteractions(collector);

        ProceedingJoinPoint orderJoinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method orderMethod = Fixture.class.getDeclaredMethod("orderOperation");
        when(orderJoinPoint.getSignature()).thenReturn(signature);
        when(orderJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(orderJoinPoint.proceed()).thenReturn("order-result");
        when(signature.getMethod()).thenReturn(orderMethod);
        when(signature.getDeclaringTypeName()).thenReturn(Fixture.class.getName());

        Object orderResult = aspect.around(orderJoinPoint, annotation("orderOperation"));
        assertThat(orderResult).isEqualTo("order-result");
        verify(collector).collect(org.mockito.ArgumentMatchers.argThat(event ->
                "order".equals(event.getModule()) && "save_order".equals(event.getAction())));
    }

    private CollectLog annotation(String methodName) throws NoSuchMethodException {
        return Fixture.class.getDeclaredMethod(methodName).getAnnotation(CollectLog.class);
    }

    static class Fixture {
        @CollectLog(module = "customer", action = "update_customer")
        void customerOperation() {
        }

        @CollectLog(module = "order", action = "save_order")
        void orderOperation() {
        }
    }
}
