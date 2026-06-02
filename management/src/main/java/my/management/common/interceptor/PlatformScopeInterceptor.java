package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class PlatformScopeInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (request.getServletPath().startsWith("/platform/") && !isDeveloperAccount()) {
            writeErrorResponse(response, "仅开发者可访问租户管理");
            return false;
        }
        return true;
    }

    private boolean isDeveloperAccount() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Set<String> permCodes = TenantPermissionContext.getPermCodes();
        return "super".equalsIgnoreCase(String.valueOf(tenantCode).trim())
                && (permCodes.contains("super") || permCodes.contains("developer:super"));
    }

    private void writeErrorResponse(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        try (var writer = response.getWriter()) {
            objectMapper.writeValue(writer, Result.fail(403, msg));
            writer.flush();
        }
    }
}
