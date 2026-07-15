package my.hive.shared.tenant;

import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.domain.tenant.service.TenantLicenseService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

@Aspect
@Component
public class TenantFeatureAspect {

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Around("@within(my.hive.shared.tenant.RequireTenantFeature) || @annotation(my.hive.shared.tenant.RequireTenantFeature)")
    public Object requireFeature(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireTenantFeature requireTenantFeature = resolveFeatureAnnotation(joinPoint);
        if (requireTenantFeature == null) {
            return joinPoint.proceed();
        }
        tenantLicenseService.requireFeatureEnabled(
                TenantPermissionContext.getTenantCode(),
                requireTenantFeature.value(),
                requireTenantFeature.message()
        );
        return joinPoint.proceed();
    }

    private RequireTenantFeature resolveFeatureAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireTenantFeature annotation = AnnotationUtils.findAnnotation(method, RequireTenantFeature.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = joinPoint.getTarget();
        return target == null ? null : AnnotationUtils.findAnnotation(target.getClass(), RequireTenantFeature.class);
    }
}
