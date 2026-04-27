package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 平台访问范围拦截器。
 * super 账号只用于平台租户管理，不进入租户内业务；普通租户也不能访问平台接口。
 */
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
        boolean platformPath = path.startsWith("/platform/");

        if (superTenant && !tenantManagePath) {
            writeErrorResponse(response, "平台超管仅允许访问租户管理");
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
