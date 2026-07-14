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
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(collectLog)")
    public Object around(ProceedingJoinPoint joinPoint, CollectLog collectLog) throws Throwable {
        if (!properties.isEnabled()) {
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
            if (collectLog.recordResult()) {
                event.setResultJson(sanitizer.toSafeJson(result));
            }
            return result;
        } catch (Throwable throwable) {
            event.setSuccess(false);
            event.setErrorType(throwable.getClass().getName());
            event.setErrorMessage(throwable.getMessage());
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
        event.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        event.setTenantCode(TenantPermissionContext.getTenantCode());
        event.setUserId(TenantPermissionContext.getUserId());
        event.setModule(collectLog.module());
        event.setAction(collectLog.action());
        event.setBizType(collectLog.bizType());
        event.setBizNo(resolveBizNo(method, joinPoint.getArgs(), collectLog.bizNo()));
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

    private String resolveBizNo(Method method, Object[] args, String expression) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
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
        event.setClientIp(resolveClientIp(request));
        event.setUserAgent(request.getHeader("User-Agent"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
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
