package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.management.common.context.TenantPermissionContext;
import my.management.common.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantCode = request.getHeader("Tenant-Code");
        String userIdStr = request.getHeader("User-Id");

        if (tenantCode == null || tenantCode.isBlank()) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, 400, "Tenant-Code is required");
            return false;
        }
        if (userIdStr == null || userIdStr.isBlank()) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, 400, "User-Id is required");
            return false;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException ex) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, 400, "User-Id is invalid");
            return false;
        }

        TenantPermissionContext.init(tenantCode, userId, Set.of());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantPermissionContext.clear();
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus httpStatus, Integer bizCode, String msg) throws Exception {
        Result<Void> errorResult = Result.fail(bizCode, msg);
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(httpStatus.value());
        try (var writer = response.getWriter()) {
            objectMapper.writeValue(writer, errorResult);
            writer.flush();
        }
    }
}
