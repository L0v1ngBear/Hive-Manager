package my.hive.shared.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.api.order.OrderController;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderShipmentSaveRequest;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.dto.Result;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogEvent;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    void resolvesBusinessNumberFromSuccessfulResult() throws Throwable {
        OperationLogProperties properties = new OperationLogProperties();
        properties.setRecordedModules(Set.of("order"));
        OperationLogCollector collector = mock(OperationLogCollector.class);
        SensitiveDataSanitizer sanitizer = mock(SensitiveDataSanitizer.class);
        OperationLogAspect aspect = new OperationLogAspect(properties, collector, sanitizer);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = Fixture.class.getDeclaredMethod("createOrder");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("SO-1001");
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(Fixture.class.getName());

        assertThat(aspect.around(joinPoint, annotation("createOrder"))).isEqualTo("SO-1001");
        verify(collector).collect(org.mockito.ArgumentMatchers.argThat(event ->
                "SO-1001".equals(event.getBizNo())));
    }

    @Test
    void masksNestedTrackingNumbersForOrderCreateAndSaveWithoutMutatingApiPayload() throws Throwable {
        OperationLogProperties properties = new OperationLogProperties();
        properties.setRecordedModules(Set.of("order"));
        OperationLogCollector collector = mock(OperationLogCollector.class);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(new ObjectMapper(), properties);
        OperationLogAspect aspect = new OperationLogAspect(properties, collector, sanitizer);
        SalesOrderSaveRequest request = orderRequest("TRACKING-NO-1234567890");

        Result<String> createResult = Result.success("SO-1001");
        Object returnedCreateResult = invoke(aspect,
                OrderController.class.getDeclaredMethod("create", SalesOrderSaveRequest.class),
                new Object[]{request}, createResult);
        Result<Void> saveResult = Result.success(null);
        Object returnedSaveResult = invoke(aspect,
                OrderController.class.getDeclaredMethod("replace", String.class, SalesOrderSaveRequest.class),
                new Object[]{"SO-1001", request}, saveResult);

        assertThat(returnedCreateResult).isSameAs(createResult);
        assertThat(returnedSaveResult).isSameAs(saveResult);
        assertThat(request.getShipments().get(0).getTrackingNo()).isEqualTo("TRACKING-NO-1234567890");
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(collector, times(2)).collect(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(event -> assertThat(event.getArgsJson())
                        .contains("\"trackingNo\":\"******\"")
                        .doesNotContain("TRACKING-NO-1234567890"));
    }

    private Object invoke(OperationLogAspect aspect, Method method, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(result);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(method.getDeclaringClass().getName());
        return aspect.around(joinPoint, method.getAnnotation(CollectLog.class));
    }

    private SalesOrderSaveRequest orderRequest(String trackingNo) {
        SalesOrderShipmentSaveRequest shipment = new SalesOrderShipmentSaveRequest();
        shipment.setLogisticsCompany("SF Express");
        shipment.setTrackingNo(trackingNo);
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setCustomerName("Test Customer");
        request.setProjectName("Test Project");
        request.setShipments(List.of(shipment));
        return request;
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

        @CollectLog(module = "order", action = "create_order", resultBizNo = "#result")
        String createOrder() {
            return "SO-1001";
        }
    }
}
