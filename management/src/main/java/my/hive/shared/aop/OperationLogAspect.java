package my.hive.shared.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.context.OperationLogSkipContext;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogEvent;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.SensitiveDataSanitizer;
import my.hive.shared.web.RequestTraceFilter;
import my.hive.shared.web.TrustedClientIpResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 操作日志 AOP 切面。
 * 通过 @CollectLog 统一采集业务排查信息，避免每个接口重复手写日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogProperties properties;
    private final OperationLogCollector collector;
    private final SensitiveDataSanitizer sanitizer;
    private final TrustedClientIpResolver trustedClientIpResolver;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(collectLog)")
    public Object around(ProceedingJoinPoint joinPoint, CollectLog collectLog) throws Throwable {
        if (!properties.isEnabled() || !properties.shouldRecordModule(collectLog.module())) {
            try {
                return joinPoint.proceed();
            } finally {
                OperationLogSkipContext.clear();
            }
        }

        long startTime = System.currentTimeMillis();
        OperationLogEvent event = buildBaseEvent(joinPoint, collectLog);
        try {
            Object result = joinPoint.proceed();
            event.setSuccess(true);
            if (!collectLog.resultBizNo().isBlank()) {
                event.setBizNo(resolveBizNo(methodOf(joinPoint), joinPoint.getArgs(), collectLog.resultBizNo(), result));
            }
            if (collectLog.recordResult()) {
                event.setResultJson(sanitizer.toSafeJson(result));
            }
            return result;
        } catch (Throwable throwable) {
            event.setSuccess(false);
            event.setErrorType(throwable.getClass().getName());
            event.setErrorMessage(sanitizer.toSafeExceptionMessage(throwable));
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            event.setDurationMs(duration);
            event.setSlow(duration >= resolveSlowThreshold(collectLog));
            event.setLogLevel(resolveLogLevel(event));
            try {
                if (!OperationLogSkipContext.shouldSkip()) {
                    collector.collect(event);
                }
            } finally {
                OperationLogSkipContext.clear();
            }
        }
    }

    private OperationLogEvent buildBaseEvent(ProceedingJoinPoint joinPoint, CollectLog collectLog) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLogEvent event = new OperationLogEvent();
        event.setTraceId(resolveTraceId());
        event.setTenantCode(TenantPermissionContext.getTenantCode());
        event.setUserId(TenantPermissionContext.getUserId());
        event.setModule(collectLog.module());
        event.setAction(collectLog.action());
        event.setBizType(collectLog.bizType());
        event.setBizNo(resolveBizNo(method, joinPoint.getArgs(), collectLog.bizNo(), null));
        event.setDescription(collectLog.description());
        event.setClassName(signature.getDeclaringTypeName());
        event.setMethodName(method.getName());
        event.setCreateTime(LocalDateTime.now());
        if (collectLog.recordArgs()) {
            event.setArgsJson(sanitizer.toSafeJson(joinPoint.getArgs()));
        }
        fillRequestInfo(event);
        return event;
    }

    private Method methodOf(ProceedingJoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod();
    }

    private String resolveBizNo(Method method, Object[] args, String expression, Object result) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", result);
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            for (int i = 0; i < args.length; i += 1) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
                if (parameterNames != null && i < parameterNames.length) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            Object value = expressionParser.parseExpression(expression).getValue(context);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ex) {
            return "";
        }
    }

    private void fillRequestInfo(OperationLogEvent event) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        event.setRequestMethod(request.getMethod());
        event.setRequestUri(request.getRequestURI());
        event.setClientIp(trustedClientIpResolver.resolve(request));
        event.setUserAgent(request.getHeader("User-Agent"));
    }

    /**
     * A business audit entry must be joinable with the request, exception and
     * external-call entries produced by the same HTTP invocation.  Retain a
     * generated id for non-web work such as a scheduled job.
     */
    private String resolveTraceId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object traceId = attributes.getRequest().getAttribute(RequestTraceFilter.TRACE_ATTRIBUTE);
            if (traceId != null && !String.valueOf(traceId).isBlank()) {
                return String.valueOf(traceId);
            }
        }
        String traceId = MDC.get(RequestTraceFilter.MDC_TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private long resolveSlowThreshold(CollectLog collectLog) {
        return collectLog.slowThresholdMs() >= 0 ? collectLog.slowThresholdMs() : properties.getSlowThresholdMs();
    }

    private String resolveLogLevel(OperationLogEvent event) {
        if (Boolean.FALSE.equals(event.getSuccess())) {
            return "ERROR";
        }
        if (Boolean.TRUE.equals(event.getSlow())) {
            return "WARN";
        }
        return "INFO";
    }
}
