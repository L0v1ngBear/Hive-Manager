package my.hive.shared.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import my.hive.domain.auth.model.AuthReason;
import my.hive.shared.dto.Result;
import my.hive.shared.event.SystemEvent;
import my.hive.shared.event.SystemEventPublisher;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionPreservesReasonAndSafeData() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SystemEventPublisher> provider = mock(ObjectProvider.class);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider, sanitizer);
        BusinessException exception = new BusinessException(
                403,
                AuthReason.EMPLOYEE_NOT_FOUND,
                "管理员尚未添加该手机号",
                Map.of("phoneVerificationTicket", "ticket-1")
        );

        ResponseEntity<Result<Object>> response = handler.handleBusinessException(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReason()).isEqualTo(AuthReason.EMPLOYEE_NOT_FOUND);
        assertThat(response.getBody().getMsg()).isEqualTo("管理员尚未添加该手机号");
        assertThat(response.getBody().getData())
                .isEqualTo(Map.of("phoneVerificationTicket", "ticket-1"));
    }

    @Test
    void legacyBusinessExceptionHasNoReason() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SystemEventPublisher> provider = mock(ObjectProvider.class);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider, sanitizer);

        ResponseEntity<Result<Object>> response = handler.handleBusinessException(
                new BusinessException(400, "请检查输入"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReason()).isNull();
        assertThat(response.getBody().getMsg()).isEqualTo("请检查输入");
    }

    @Test
    void hidesTrackingConstraintMessageFromResponseEventAndApplicationLog(CapturedOutput output) {
        SystemEventPublisher publisher = mock(SystemEventPublisher.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SystemEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(publisher);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider, sanitizer);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/orders/SO-1");
        String rawMessage = "Duplicate entry 'TENANT-ORDER-SF123' for key 'uk_order_shipment_tracking'";

        ResponseEntity<Result<Void>> response = handler.handleGlobalException(
                new DataIntegrityViolationException(rawMessage), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMsg()).isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE);
        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getContent()).isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE);
        assertThat(new ObjectMapper().valueToTree(eventCaptor.getValue().getDetail()).toString())
                .doesNotContain("SF123")
                .doesNotContain("TENANT-ORDER");
        assertThat(output.getAll())
                .doesNotContain("SF123")
                .doesNotContain("TENANT-ORDER");
    }

    @Test
    void sanitizesConstraintShapedBusinessMessageButPreservesOrdinaryBusinessMessage(CapturedOutput output) {
        @SuppressWarnings("unchecked")
        ObjectProvider<SystemEventPublisher> provider = mock(ObjectProvider.class);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider, sanitizer);
        String rawMessage = "Duplicate entry 'TENANT-ORDER-SF123' for key 'uk_order_shipment_tracking'";

        ResponseEntity<Result<Object>> constraintResponse =
                handler.handleBusinessException(new BusinessException(409, rawMessage));
        ResponseEntity<Result<Object>> ordinaryResponse =
                handler.handleBusinessException(new BusinessException(409, "发货记录已被修改"));

        assertThat(constraintResponse.getBody()).isNotNull();
        assertThat(constraintResponse.getBody().getMsg()).isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE);
        assertThat(ordinaryResponse.getBody()).isNotNull();
        assertThat(ordinaryResponse.getBody().getMsg()).isEqualTo("发货记录已被修改");
        assertThat(output.getAll())
                .doesNotContain("SF123")
                .doesNotContain("TENANT-ORDER")
                .contains("发货记录已被修改");
    }

    @Test
    void illegalArgumentResponseDoesNotExposeFrameworkEnglishMessage() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SystemEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(
                new ObjectMapper(), new OperationLogProperties());
        GlobalExceptionHandler handler = new GlobalExceptionHandler(provider, sanitizer);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Result<Void>> response = handler.handleIllegalArgException(
                new IllegalArgumentException("Failed to convert value"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMsg()).isEqualTo("参数格式错误，请检查后重试");
        assertThat(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE)
                .isEqualTo("数据违反唯一性或完整性约束，请检查后重试");
    }
}
