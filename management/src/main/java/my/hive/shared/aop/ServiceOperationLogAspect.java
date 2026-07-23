package my.hive.shared.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.log.SensitiveDataSanitizer;
import my.hive.shared.log.ServiceOperationLogProperties;
import my.hive.shared.web.RequestTraceFilter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 统一记录 HTTP 接口调用链中的公共 Service 方法。
 * 日志只输出定位字段，不序列化入参和返回值，避免敏感信息及大对象进入日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ServiceOperationLogAspect {

    private static final ThreadLocal<Integer> CALL_DEPTH = new ThreadLocal<>();

    private final ServiceOperationLogProperties properties;
    private final SensitiveDataSanitizer sanitizer;

    @Around("execution(public * my.hive..*(..)) && @within(org.springframework.stereotype.Service)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()
                || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        int depth = enterCall();
        String traceId = resolveTraceId(request);
        String serviceName = AopUtils.getTargetClass(joinPoint.getTarget()).getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String tenantCode = displayValue(TenantPermissionContext.getTenantCode());
        String userId = displayValue(TenantPermissionContext.getUserId());
        long startedAt = System.nanoTime();

        if (properties.isIncludeStart()) {
            log.info(
                    "HIVE_SERVICE_LOG traceId={}, result=STARTED, httpMethod={}, uri={}, service={}.{}, "
                            + "tenantCode={}, userId={}, depth={}",
                    traceId, request.getMethod(), request.getRequestURI(), serviceName, methodName,
                    tenantCode, userId, depth);
        }

        try {
            Object result = joinPoint.proceed();
            long durationMs = elapsedMillis(startedAt);
            boolean slow = durationMs >= Math.max(properties.getSlowThresholdMs(), 0L);
            if (slow) {
                log.warn(
                        "HIVE_SERVICE_LOG traceId={}, result=SUCCESS, httpMethod={}, uri={}, service={}.{}, "
                                + "tenantCode={}, userId={}, depth={}, durationMs={}, slow=true",
                        traceId, request.getMethod(), request.getRequestURI(), serviceName, methodName,
                        tenantCode, userId, depth, durationMs);
            } else {
                log.info(
                        "HIVE_SERVICE_LOG traceId={}, result=SUCCESS, httpMethod={}, uri={}, service={}.{}, "
                                + "tenantCode={}, userId={}, depth={}, durationMs={}, slow=false",
                        traceId, request.getMethod(), request.getRequestURI(), serviceName, methodName,
                        tenantCode, userId, depth, durationMs);
            }
            return result;
        } catch (Throwable throwable) {
            long durationMs = elapsedMillis(startedAt);
            String exceptionMessage = properties.isIncludeExceptionMessage()
                    ? safeExceptionMessage(throwable)
                    : "-";
            if (throwable instanceof BusinessException) {
                log.warn(
                        "HIVE_SERVICE_LOG traceId={}, result=FAILED, httpMethod={}, uri={}, service={}.{}, "
                                + "tenantCode={}, userId={}, depth={}, durationMs={}, exceptionType={}, exceptionMessage={}",
                        traceId, request.getMethod(), request.getRequestURI(), serviceName, methodName,
                        tenantCode, userId, depth, durationMs, throwable.getClass().getSimpleName(), exceptionMessage);
            } else {
                log.error(
                        "HIVE_SERVICE_LOG traceId={}, result=FAILED, httpMethod={}, uri={}, service={}.{}, "
                                + "tenantCode={}, userId={}, depth={}, durationMs={}, exceptionType={}, exceptionMessage={}",
                        traceId, request.getMethod(), request.getRequestURI(), serviceName, methodName,
                        tenantCode, userId, depth, durationMs, throwable.getClass().getSimpleName(), exceptionMessage);
            }
            throw throwable;
        } finally {
            exitCall(depth);
        }
    }

    private int enterCall() {
        Integer current = CALL_DEPTH.get();
        int depth = current == null ? 0 : current;
        CALL_DEPTH.set(depth + 1);
        return depth;
    }

    private void exitCall(int depth) {
        if (depth <= 0) {
            CALL_DEPTH.remove();
        } else {
            CALL_DEPTH.set(depth);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestTraceFilter.TRACE_ATTRIBUTE);
        if (attribute != null) {
            return String.valueOf(attribute);
        }
        String mdcTraceId = MDC.get(RequestTraceFilter.MDC_TRACE_ID);
        return mdcTraceId == null || mdcTraceId.isBlank() ? "-" : mdcTraceId;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String safeExceptionMessage(Throwable throwable) {
        String message = sanitizer.toSafeExceptionMessage(throwable);
        if (message == null || message.isBlank()) {
            return "-";
        }
        String oneLine = message.replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        int maxLength = Math.max(properties.getMaxExceptionMessageLength(), 0);
        if (maxLength == 0) {
            return "-";
        }
        return oneLine.length() <= maxLength ? oneLine : oneLine.substring(0, maxLength) + "...[truncated]";
    }

    private String displayValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
