package my.hive.shared.exception;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.Result;
import my.hive.shared.event.SystemEvent;
import my.hive.shared.event.SystemEventPublisher;
import my.hive.shared.log.SensitiveDataSanitizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectProvider<SystemEventPublisher> systemEventPublisherProvider;
    private final SensitiveDataSanitizer sanitizer;

    @PostConstruct
    public void init() {
        log.info("===== GlobalExceptionHandler loaded =====");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleRequestBodyValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        String errorMsg = fieldErrors.stream()
                .map(error -> String.format("字段[%s]：%s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("；"));
        log.warn("request body validation failed: {}", errorMsg);
        return new ResponseEntity<>(Result.fail(400, errorMsg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleRequestParamValidException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMsg = violations.stream()
                .map(violation -> {
                    String paramName = violation.getPropertyPath().toString();
                    if (paramName.contains(".")) {
                        paramName = paramName.substring(paramName.lastIndexOf('.') + 1);
                    }
                    return String.format("参数[%s]：%s", paramName, violation.getMessage());
                })
                .collect(Collectors.joining("；"));
        log.warn("request parameter validation failed: {}", errorMsg);
        return new ResponseEntity<>(Result.fail(400, errorMsg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        if (sanitizer.isDataConstraintViolation(e)) {
            log.warn("business exception caused by a database constraint");
            return new ResponseEntity<>(
                    Result.fail(e.getCode(), sanitizer.toSafeExceptionMessage(e)), HttpStatus.OK);
        }
        log.warn("business exception: {}", e.getMsg());
        return new ResponseEntity<>(Result.fail(e.getCode(), e.getMsg()), HttpStatus.OK);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("illegal argument exception: {}", e.getMessage(), e);
        publishExceptionEvent("ILLEGAL_ARGUMENT_EXCEPTION", "接口参数格式异常", e, request);
        return new ResponseEntity<>(Result.fail(400, "参数格式错误：" + e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<Void>> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("null pointer exception", e);
        publishExceptionEvent("NULL_POINTER_EXCEPTION", "接口发生空指针异常", e, request);
        return new ResponseEntity<>(Result.fail(500, "服务器内部错误，请稍后重试"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGlobalException(Exception e, HttpServletRequest request) {
        if (sanitizer.isDataConstraintViolation(e)) {
            String safeMessage = sanitizer.toSafeExceptionMessage(e);
            log.error("database constraint violation");
            publishExceptionEvent("DATA_CONSTRAINT_VIOLATION", "Database constraint violation", e, request);
            return new ResponseEntity<>(Result.fail(409, safeMessage), HttpStatus.CONFLICT);
        }
        log.error("system internal exception", e);
        publishExceptionEvent("GLOBAL_EXCEPTION", "接口发生未处理异常", e, request);
        return new ResponseEntity<>(Result.fail(500, "服务器内部错误，请稍后重试"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void publishExceptionEvent(String eventType, String title, Exception exception, HttpServletRequest request) {
        try {
            String safeMessage = sanitizer.toSafeExceptionMessage(exception);
            SystemEventPublisher publisher = systemEventPublisherProvider.getIfAvailable();
            if (publisher == null) {
                return;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("errorType", exception.getClass().getName());
            detail.put("errorMessage", safeMessage);
            if (request != null) {
                detail.put("method", request.getMethod());
                detail.put("path", request.getRequestURI());
                detail.put("query", request.getQueryString());
            }
            Long userId = TenantPermissionContext.getUserId();
            detail.put("userId", userId);

            publisher.publish(SystemEvent.builder()
                    .eventType(eventType)
                    .level("ERROR")
                    .tenantCode(TenantPermissionContext.getTenantCode())
                    .module("global-exception")
                    .title(title)
                    .content(safeMessage)
                    .bizType("http-request")
                    .bizNo(request == null ? null : request.getRequestURI())
                    .detail(detail)
                    .build());
        } catch (Exception publishEx) {
            if (sanitizer.isDataConstraintViolation(publishEx)) {
                log.warn("publish global exception event failed due to a database constraint");
            } else {
                log.warn("publish global exception event failed", publishEx);
            }
        }
    }
}
