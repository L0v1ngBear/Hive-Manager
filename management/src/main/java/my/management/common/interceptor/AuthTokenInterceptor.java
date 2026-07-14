package my.management.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.auth.AuthUserInfo;
import my.hive.shared.auth.AuthenticatedSessionService;
import my.hive.shared.auth.TokenService;
import my.hive.shared.dto.Result;
import my.hive.shared.tenant.TenantIsolationSupport;
import my.hive.shared.tenant.TenantContext;
import my.hive.shared.utils.ResponseEncryptUtil;
import my.hive.shared.utils.TokenUtil;
import my.management.common.tenant.BoundedTenantProperties;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.auth.mapper.AuthMapper;
import my.management.module.auth.model.vo.LoginUserRow;
import my.management.module.employee.model.enums.EmployeeStatusEnum;
import my.management.module.tenant.service.TenantLicenseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private static final String PLATFORM_TENANT_CODE = "super";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AuthMapper authMapper;

    @Resource
    private AuthenticatedSessionService authenticatedSessionService;

    @Resource
    private TokenService tokenService;

    @Resource
    private TenantContext tenantContext;

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

        AuthUserInfo authUserInfo = authenticatedSessionService.authenticate(authorization.substring(7).trim());
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

        LoginUserRow accountState = authMapper.selectLoginUserByUserIdAndTenantCode(
                authUserInfo.getUserId(), tenantCode);
        if (accountState == null
                || !isUsableEmployeeStatus(accountState.getUserStatus())
                || accountState.getPermissionVersion() == null
                || accountState.getPermissionVersion() <= 0
                || accountState.getAuthVersion() == null
                || accountState.getAuthVersion() <= 0
                || !Objects.equals(authUserInfo.getAuthVersion(), accountState.getAuthVersion())) {
            tenantIsolationSupport.clearTenantDatasource();
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, 401, "登录状态已失效，请重新登录");
            return false;
        }
        Long currentPermissionVersion = accountState.getPermissionVersion();
        Long currentAuthVersion = accountState.getAuthVersion();

        if (!platformTenant) {
            try {
                tenantLicenseService.ensureTenantUsable(tenantCode);
            } catch (my.hive.shared.exception.BusinessException ex) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, ex.getCode(), ex.getMsg());
                return false;
            }
        }

        Set<String> permCodes = permissionCacheUtil.get(
                tenantCode, authUserInfo.getUserId(), currentPermissionVersion);
        if (permCodes == null) {
            List<String> permissionList = authMapper.selectPermCodesByUserIdAndTenantCode(authUserInfo.getUserId(), tenantCode);
            permCodes = new LinkedHashSet<>(permissionList == null ? List.of() : permissionList);
            permissionCacheUtil.put(
                    tenantCode, authUserInfo.getUserId(), currentPermissionVersion, permCodes);
        }
        tenantContext.initialize(tenantCode, authUserInfo.getUserId(), permCodes);
        maybeRenewToken(response, authUserInfo, currentAuthVersion);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tenantIsolationSupport.clearTenantDatasource();
        tenantContext.clear();
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

    private void maybeRenewToken(HttpServletResponse response,
                                 AuthUserInfo authUserInfo,
                                 Long currentAuthVersion) {
        if (!tokenRenewEnabled || response.isCommitted() || !tokenService.shouldRenew(authUserInfo, tokenRenewBeforeMinutes)) {
            return;
        }
        String renewedToken = tokenService.create(
                authUserInfo.getUserId(), authUserInfo.getTenantCode(), currentAuthVersion);
        AuthUserInfo renewedUserInfo = tokenService.parse(renewedToken);
        if (renewedUserInfo == null || renewedUserInfo.getExpireAt() == null) {
            return;
        }
        String renewedResponseKey = responseEncryptUtil.buildResponseKey(renewedToken);
        response.setHeader(TokenUtil.HEADER_RENEWED_TOKEN, renewedToken);
        response.setHeader(TokenUtil.HEADER_RENEWED_EXPIRE_AT, String.valueOf(renewedUserInfo.getExpireAt()));
        response.setHeader(TokenUtil.HEADER_RENEWED_RESPONSE_KEY, renewedResponseKey);
    }

    private boolean isUsableEmployeeStatus(Integer status) {
        return Objects.equals(status, EmployeeStatusEnum.ACTIVE.getCode())
                || Objects.equals(status, EmployeeStatusEnum.PROBATION.getCode());
    }
}
