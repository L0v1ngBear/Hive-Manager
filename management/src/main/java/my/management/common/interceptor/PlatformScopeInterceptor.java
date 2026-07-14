package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PlatformScopeInterceptor implements HandlerInterceptor {

    private static final String PLATFORM_TENANT_CODE = "super";
    private static final String PLATFORM_PATH_PREFIX = "/platform/";
    private static final String INITIAL_PASSWORD_PATH = "/auth/initial-password";
    private static final String UPLOAD_PATH_PREFIX = "/uploads/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = normalizePath(request.getServletPath());
        if (isPlatformTenant()) {
            if (isPlatformAllowedPath(path)) {
                return true;
            }
            writeErrorResponse(response, "平台账号仅可访问租户管理");
            return false;
        }

        if (path.startsWith(PLATFORM_PATH_PREFIX)) {
            writeErrorResponse(response, "当前账号无权访问租户管理");
            return false;
        }
        return true;
    }

    private boolean isPlatformAllowedPath(String path) {
        return path.startsWith(PLATFORM_PATH_PREFIX)
                || INITIAL_PASSWORD_PATH.equals(path)
                || path.startsWith(UPLOAD_PATH_PREFIX);
    }

    private boolean isPlatformTenant() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        return PLATFORM_TENANT_CODE.equalsIgnoreCase(String.valueOf(tenantCode).trim());
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.trim();
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
