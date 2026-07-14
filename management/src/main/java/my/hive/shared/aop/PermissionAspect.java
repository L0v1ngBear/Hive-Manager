package my.hive.shared.aop;

import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
/**
 * PermissionAspect 属于共享后端通用能力层，统一拦截 @RequirePermission 标注的方法。
 * 管理端后端和小程序后端都会依赖这个切面，确保权限控制不只停留在前端菜单隐藏。
 */
@Component
@Aspect
public class PermissionAspect {

    @Pointcut("@annotation(my.hive.shared.annotation.RequirePermission)")
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

        String[] permCodes = annotation.value();
        if (permCodes == null || permCodes.length == 0) {
            return;
        }
        for (String permCode : permCodes) {
            if (!StringUtils.hasText(permCode)) {
                continue;
            }
            if (TenantPermissionContext.hasPermission(permCode)) {
                return;
            }
        }
        throw new BusinessException(403, buildPermissionDeniedMessage(annotation.message(), permCodes));
    }

    private String buildPermissionDeniedMessage(String message, String[] permCodes) {
        String joinedPermCodes = String.join(" 或 ", permCodes);
        return buildPermissionDeniedMessage(message, joinedPermCodes);
    }

    private String buildPermissionDeniedMessage(String message, String permCode) {
        String normalizedMessage = StringUtils.hasText(message) ? message.trim() : "";
        if (!StringUtils.hasText(normalizedMessage) || "无权限访问".equals(normalizedMessage)) {
            return "当前账号没有权限执行该操作，请联系管理员在角色管理中分配权限（权限码：" + permCode + "）";
        }
        if (normalizedMessage.contains("联系管理员") || normalizedMessage.contains("角色管理")) {
            return normalizedMessage;
        }
        return normalizedMessage + "，请联系管理员在角色管理中分配权限";
    }
}
