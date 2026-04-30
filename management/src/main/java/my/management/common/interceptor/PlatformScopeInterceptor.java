package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PlatformScopeInterceptor implements HandlerInterceptor {

    private static final String SUPER_TENANT_CODE = "super";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        String tenantCode = TenantPermissionContext.getTenantCode();
        boolean superTenant = SUPER_TENANT_CODE.equalsIgnoreCase(tenantCode);
        boolean tenantManagePath = path.startsWith("/platform/tenant");
        boolean platformOpsPath = path.startsWith("/platform/operation-log")
                || path.startsWith("/platform/system-event");
        boolean platformPath = path.startsWith("/platform/");

        if (superTenant && !tenantManagePath && !platformOpsPath) {
            writeErrorResponse(response, "平台超管仅允许访问租户管理和平台运维接口");
            return false;
        }
        if (!superTenant && platformPath) {
            writeErrorResponse(response, "租户账号不能访问平台管理接口");
            return false;
        }
        return true;
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
