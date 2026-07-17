package my.hive.shared.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

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

        ResponseEntity<Result<Void>> constraintResponse =
                handler.handleBusinessException(new BusinessException(409, rawMessage));
        ResponseEntity<Result<Void>> ordinaryResponse =
                handler.handleBusinessException(new BusinessException(409, "Shipment has been modified"));

        assertThat(constraintResponse.getBody()).isNotNull();
        assertThat(constraintResponse.getBody().getMsg()).isEqualTo(SensitiveDataSanitizer.DATA_CONSTRAINT_MESSAGE);
        assertThat(ordinaryResponse.getBody()).isNotNull();
        assertThat(ordinaryResponse.getBody().getMsg()).isEqualTo("Shipment has been modified");
        assertThat(output.getAll())
                .doesNotContain("SF123")
                .doesNotContain("TENANT-ORDER")
                .contains("Shipment has been modified");
    }
}
