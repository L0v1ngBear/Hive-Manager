package my.management.common.aop;

import my.management.common.annotation.RequirePermission;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionAspect {

    @Pointcut("@annotation(my.management.common.annotation.RequirePermission)")
    public void permissionPointcut() {
    }

    @Before("permissionPointcut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return;
        }

        String permCode = annotation.value();
        if (!TenantPermissionContext.hasPermission(permCode)) {
            throw new BusinessException(403, annotation.message());
        }
    }
}
