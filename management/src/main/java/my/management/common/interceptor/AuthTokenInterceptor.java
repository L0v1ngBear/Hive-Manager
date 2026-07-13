package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.auth.AuthUserInfo;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.Result;
import my.hive.common.tenant.TenantIsolationSupport;
import my.hive.common.utils.ResponseEncryptUtil;
import my.hive.common.utils.TokenUtil;
import my.management.common.tenant.BoundedTenantProperties;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.auth.mapper.AuthMapper;
import my.management.module.tenant.service.TenantLicenseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private static final String PLATFORM_TENANT_CODE = "super";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AuthMapper authMapper;

    @Resource
    private PermissionCacheUtil permissionCacheUtil;

    @Resource
    private TenantIsolationSupport tenantIsolationSupport;

    @Resource
    private ResponseEncryptUtil responseEncryptUtil;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private BoundedTenantProperties boundedTenantProperties;

    @Value("${auth.token.renew-enabled:true}")
    private boolean tokenRenewEnabled;

    @Value("${auth.token.renew-before-minutes:120}")
    private long tokenRenewBeforeMinutes;

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
        if (authUserInfo == null || authUserInfo.getUserId() == null
                || authUserInfo.getTenantCode() == null || authUserInfo.getTenantCode().isBlank()) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, 401, "登录状态已失效，请重新登录");
            return false;
        }

        String tenantCode = authUserInfo.getTenantCode();
        boolean platformTenant = isPlatformTenant(tenantCode);
        if (!platformTenant && !boundedTenantProperties.isTenantAllowed(tenantCode)) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, 403, "当前组织不在系统允许范围内");
            return false;
        }

        // FIELD mode is a no-op today; DATABASE mode can route after tenant identity is trusted.
        tenantIsolationSupport.bindTenantDatasource(tenantCode);

        if (!platformTenant) {
            try {
                tenantLicenseService.ensureTenantUsable(tenantCode);
            } catch (my.hive.common.exception.BusinessException ex) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, ex.getCode(), ex.getMsg());
                return false;
            }
        }

        Set<String> permCodes = permissionCacheUtil.get(tenantCode, authUserInfo.getUserId());
        if (permCodes == null) {
            List<String> permissionList = authMapper.selectPermCodesByUserIdAndTenantCode(authUserInfo.getUserId(), tenantCode);
            permCodes = new LinkedHashSet<>(permissionList == null ? List.of() : permissionList);
            permissionCacheUtil.put(tenantCode, authUserInfo.getUserId(), permCodes);
        }
        TenantPermissionContext.init(tenantCode, authUserInfo.getUserId(), permCodes);
        maybeRenewToken(response, authUserInfo);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tenantIsolationSupport.clearTenantDatasource();
        TenantPermissionContext.clear();
    }

    private boolean isPlatformTenant(String tenantCode) {
        return PLATFORM_TENANT_CODE.equalsIgnoreCase(String.valueOf(tenantCode).trim());
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

    private void maybeRenewToken(HttpServletResponse response, AuthUserInfo authUserInfo) {
        if (!tokenRenewEnabled || response.isCommitted() || !TokenUtil.shouldRenew(authUserInfo, tokenRenewBeforeMinutes)) {
            return;
        }
        String renewedToken = TokenUtil.createToken(authUserInfo.getUserId(), authUserInfo.getTenantCode());
        AuthUserInfo renewedUserInfo = TokenUtil.parseToken(renewedToken);
        if (renewedUserInfo == null || renewedUserInfo.getExpireAt() == null) {
            return;
        }
        String renewedResponseKey = responseEncryptUtil.buildResponseKey(renewedToken);
        response.setHeader(TokenUtil.HEADER_RENEWED_TOKEN, renewedToken);
        response.setHeader(TokenUtil.HEADER_RENEWED_EXPIRE_AT, String.valueOf(renewedUserInfo.getExpireAt()));
        response.setHeader(TokenUtil.HEADER_RENEWED_RESPONSE_KEY, renewedResponseKey);
    }
}
