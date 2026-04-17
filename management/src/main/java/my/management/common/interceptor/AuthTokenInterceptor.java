package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.management.common.auth.AuthUserInfo;
import my.management.common.context.TenantPermissionContext;
import my.management.common.dto.Result;
import my.management.common.tenant.TenantIsolationSupport;
import my.management.common.utils.PermissionCacheUtil;
import my.management.common.utils.TokenUtil;
import my.management.module.auth.mapper.AuthMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
/**
 * AuthTokenInterceptor 属于管理端后端通用能力层，是请求拦截器，用于补充上下文、鉴权或租户处理。
 */
@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AuthMapper authMapper;

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    @Resource
    private TenantIsolationSupport tenantIsolationSupport;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, 401, "请先登录");
            return false;
        }

        AuthUserInfo authUserInfo = TokenUtil.parseToken(authorization.substring(7).trim());
        if (authUserInfo == null || authUserInfo.getUserId() == null || authUserInfo.getTenantCode() == null || authUserInfo.getTenantCode().isBlank()) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, 401, "登录状态已失效，请重新登录");
            return false;
        }

        Set<String> permCodes = permissionCacheUtil.get(authUserInfo.getTenantCode(), authUserInfo.getUserId());
        if (permCodes == null) {
            List<String> permissionList = authMapper.selectPermCodesByUserIdAndTenantCode(authUserInfo.getUserId(), authUserInfo.getTenantCode());
            permCodes = new LinkedHashSet<>(permissionList == null ? List.of() : permissionList);
            permissionCacheUtil.put(authUserInfo.getTenantCode(), authUserInfo.getUserId(), permCodes);
        }
        // FIELD mode keeps using the shared datasource. DATABASE mode will switch
        // to the tenant datasource here before permission/service queries run.
        tenantIsolationSupport.bindTenantDatasource(authUserInfo.getTenantCode());
        TenantPermissionContext.init(authUserInfo.getTenantCode(), authUserInfo.getUserId(), permCodes);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Always clear routing state to avoid thread reuse leaking another tenant's datasource.
        tenantIsolationSupport.clearTenantDatasource();
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
