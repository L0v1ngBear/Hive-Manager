package my.management.module.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.hive.common.privacy.PrivacyProtectionUtil;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.EncryptUtil;
import my.hive.common.utils.ResponseEncryptUtil;
import my.hive.common.utils.TokenUtil;
import my.management.module.auth.mapper.AuthMapper;
import my.management.module.auth.model.dto.InitialPasswordChangeRequest;
import my.management.module.auth.model.dto.LoginRequest;
import my.management.module.auth.model.dto.PasswordResetCodeRequest;
import my.management.module.auth.model.dto.PasswordResetRequest;
import my.management.module.auth.model.dto.WebScanConfirmRequest;
import my.management.module.auth.model.vo.LoginUserRow;
import my.management.module.auth.model.vo.LoginVO;
import my.management.module.auth.model.vo.WebScanSessionVO;
import my.management.module.auth.model.vo.WebScanStatusVO;
import my.management.common.tenant.BoundedTenantProperties;
import my.management.module.employee.model.enums.EmployeeStatusEnum;
import my.management.module.notification.sms.SmsVerificationService;
import my.management.module.tenant.service.TenantLicenseService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 管理端认证服务，负责账号密码登录以及小程序扫码网页登录。
 */
@Service
public class AuthService {

    private static final long WEB_SCAN_LOGIN_EXPIRE_SECONDS = 180L;
    private static final long PASSWORD_RESET_CODE_EXPIRE_MINUTES = 5L;
    private static final long PASSWORD_RESET_SEND_INTERVAL_SECONDS = 60L;
    private static final long PASSWORD_RESET_MAX_VERIFY_FAIL = 5L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private AuthMapper authMapper;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private ResponseEncryptUtil responseEncryptUtil;

    @Resource
    private PrivacyProtectionUtil privacyProtectionUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private TenantLicenseService tenantLicenseService;

    @Resource
    private SmsVerificationService smsVerificationService;

    @Resource
    private BoundedTenantProperties boundedTenantProperties;

    @Value("${auth.login.max-fail-count:5}")
    private Long maxFailCount;

    @Value("${auth.login.max-ip-fail-count:20}")
    private Long maxIpFailCount;

    @Value("${auth.login.lock-minutes:15}")
    private Long lockMinutes;

    public void sendPasswordResetCode(PasswordResetCodeRequest request) {
        String phone = normalizeResetPhone(request.getPhone());
        String phoneHash = privacyProtectionUtil.hashPhone(phone);
        LoginUserRow loginUser = resolvePasswordResetUser(phone, phoneHash, request.getAccount());

        String sendLockKey = passwordResetSendLockKey(phoneHash);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(sendLockKey))) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        String code = generateSmsCode();
        boolean sent = smsVerificationService.sendCode(phone, "Hive 管理端密码重置", code, PASSWORD_RESET_CODE_EXPIRE_MINUTES);
        if (!sent) {
            throw new BusinessException(503, "短信服务未配置或发送失败，请联系管理员");
        }

        String codeValue = loginUser.getUserId() + ":" + code;
        stringRedisTemplate.opsForValue().set(
                passwordResetCodeKey(phoneHash),
                codeValue,
                PASSWORD_RESET_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );
        stringRedisTemplate.opsForValue().set(sendLockKey, "1", PASSWORD_RESET_SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        stringRedisTemplate.delete(passwordResetFailKey(phoneHash));
    }

    public void resetPasswordBySmsCode(PasswordResetRequest request) {
        String phone = normalizeResetPhone(request.getPhone());
        String phoneHash = privacyProtectionUtil.hashPhone(phone);
        validateNewPassword(request.getNewPassword(), request.getConfirmPassword(), phone);

        LoginUserRow loginUser = resolvePasswordResetUser(phone, phoneHash, request.getAccount());

        ensurePasswordResetNotLocked(phoneHash);
        String storedValue = stringRedisTemplate.opsForValue().get(passwordResetCodeKey(phoneHash));
        if (storedValue == null || storedValue.isBlank()) {
            recordPasswordResetFail(phoneHash);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }

        String expectedPrefix = loginUser.getUserId() + ":";
        String expectedCode = storedValue.startsWith(expectedPrefix) ? storedValue.substring(expectedPrefix.length()) : "";
        if (!Objects.equals(expectedCode, request.getCode().trim())) {
            recordPasswordResetFail(phoneHash);
            throw new BusinessException(400, "验证码错误");
        }

        int updated = authMapper.updatePasswordByUserIdAndTenantCode(
                loginUser.getUserId(),
                loginUser.getTenantCode(),
                encryptUtil.encode(request.getNewPassword().trim())
        );
        if (updated <= 0) {
            throw new BusinessException(500, "密码修改失败，请稍后重试");
        }
        stringRedisTemplate.delete(List.of(
                passwordResetCodeKey(phoneHash),
                passwordResetSendLockKey(phoneHash),
                passwordResetFailKey(phoneHash)
        ));
    }

    public void changeInitialPassword(InitialPasswordChangeRequest request) {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (userId == null || tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(401, "请先登录后再修改密码");
        }

        LoginUserRow loginUser = authMapper.selectLoginUserByUserIdAndTenantCode(userId, tenantCode);
        if (loginUser == null || !isUsableEmployeeStatus(loginUser.getUserStatus())) {
            throw new BusinessException(403, "当前账号不可用，请联系管理员");
        }

        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword().trim();
        if (oldPassword.isEmpty() || !encryptUtil.matches(oldPassword, loginUser.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }

        validateNewPassword(request.getNewPassword(), request.getConfirmPassword(), loginUser.getPhone());
        String newPassword = request.getNewPassword().trim();
        if (encryptUtil.matches(newPassword, loginUser.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }

        int updated = authMapper.updatePasswordByUserIdAndTenantCode(
                userId,
                tenantCode,
                encryptUtil.encode(newPassword)
        );
        if (updated <= 0) {
            throw new BusinessException(500, "密码修改失败，请稍后重试");
        }
    }

    public LoginVO login(LoginRequest request, String clientIp) {
        String username = request.getUsername().trim();
        String usernamePhoneHash = privacyProtectionUtil.mayBePhoneKeyword(username) ? privacyProtectionUtil.hashPhone(username) : null;
        String accountFailKey = redisKeyBuilder.counter("auth", "management-login", "fail", "account",
                accountFailKeySegment(username, usernamePhoneHash));
        String ipFailKey = redisKeyBuilder.counter("auth", "management-login", "fail", "ip", normalizeClientIp(clientIp));
        ensureLoginNotLocked(accountFailKey, maxFailCount, "登录失败次数过多，请稍后再试");
        ensureLoginNotLocked(ipFailKey, maxIpFailCount, "当前访问过于频繁，请稍后再试");

        LoginUserRow loginUser = resolveUniqueLoginUser(authMapper.selectLoginUsers(username, usernamePhoneHash, allowedTenantCodes()));
        validatePasswordLogin(loginUser, request.getPassword(), accountFailKey, ipFailKey);
        stringRedisTemplate.delete(accountFailKey);
        stringRedisTemplate.delete(ipFailKey);
        return buildLoginVO(loginUser, request.getPassword());
    }

    public WebScanSessionVO createWebScanLoginSession() {
        String sceneKey = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() / 1000 + WEB_SCAN_LOGIN_EXPIRE_SECONDS;

        WebScanLoginRedisPayload payload = new WebScanLoginRedisPayload();
        payload.setStatus("PENDING");
        payload.setMessage("请使用已登录的小程序扫码确认");
        payload.setExpireAt(expireAt);
        saveWebScanPayload(sceneKey, payload, WEB_SCAN_LOGIN_EXPIRE_SECONDS);

        WebScanSessionVO sessionVO = new WebScanSessionVO();
        sessionVO.setSceneKey(sceneKey);
        sessionVO.setExpireAt(expireAt);
        sessionVO.setExpiresInSeconds(WEB_SCAN_LOGIN_EXPIRE_SECONDS);
        sessionVO.setQrCodeDataUrl(buildQrCodeDataUrl("HIVE_WEB_LOGIN:" + sceneKey));
        return sessionVO;
    }

    public WebScanStatusVO getWebScanLoginStatus(String sceneKey) {
        WebScanLoginRedisPayload payload = getWebScanPayload(sceneKey);
        if (payload == null) {
            WebScanStatusVO expired = new WebScanStatusVO();
            expired.setStatus("EXPIRED");
            expired.setMessage("二维码已过期，请刷新后重新扫码");
            expired.setExpireAt(0L);
            return expired;
        }

        WebScanStatusVO statusVO = new WebScanStatusVO();
        statusVO.setStatus(payload.getStatus());
        statusVO.setMessage(payload.getMessage());
        statusVO.setExpireAt(payload.getExpireAt());

        if ("CONFIRMED".equals(payload.getStatus()) && payload.getLoginInfo() != null) {
            statusVO.setLoginInfo(payload.getLoginInfo());
            payload.setStatus("USED");
            payload.setMessage("网页端已完成登录");
            saveWebScanPayload(sceneKey, payload, 10L);
        }
        return statusVO;
    }

    public void confirmWebScanLogin(WebScanConfirmRequest request) {
        String sceneKey = request.getSceneKey().trim();
        WebScanLoginRedisPayload payload = getWebScanPayload(sceneKey);
        if (payload == null) {
            throw new BusinessException(410, "二维码已过期，请刷新网页后重新扫码");
        }
        if (!"PENDING".equals(payload.getStatus())) {
            throw new BusinessException(409, "当前二维码已处理，请刷新网页后重试");
        }

        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (userId == null || tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(401, "请先登录小程序后再扫码");
        }

        LoginUserRow loginUser = authMapper.selectLoginUserByUserIdAndTenantCode(userId, tenantCode);
        if (loginUser == null || !Objects.equals(loginUser.getUserStatus(), 1)) {
            throw new BusinessException(403, "当前账号不可用于网页登录");
        }

        payload.setStatus("CONFIRMED");
        payload.setMessage("扫码确认成功，网页端正在登录");
        payload.setConfirmedUserId(userId);
        payload.setConfirmedTenantCode(tenantCode);
        payload.setConfirmedUserName(loginUser.getUserName());
        payload.setLoginInfo(buildLoginVO(loginUser, null));
        saveWebScanPayload(sceneKey, payload, WEB_SCAN_LOGIN_EXPIRE_SECONDS);
    }

    private void validatePasswordLogin(LoginUserRow loginUser, String password, String accountFailKey, String ipFailKey) {
        if (loginUser == null) {
            recordLoginFail(accountFailKey);
            recordLoginFail(ipFailKey);
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!isUsableEmployeeStatus(loginUser.getUserStatus())) {
            recordLoginFail(accountFailKey);
            recordLoginFail(ipFailKey);
            throw new BusinessException(403, employeeStatusMessage(loginUser.getUserStatus()));
        }
        if (password == null || !encryptUtil.matches(password, loginUser.getPassword())) {
            recordLoginFail(accountFailKey);
            recordLoginFail(ipFailKey);
            throw new BusinessException(401, "账号或密码错误");
        }
    }

    private void ensureLoginNotLocked(String failKey, Long limit, String message) {
        String failCountValue = stringRedisTemplate.opsForValue().get(failKey);
        if (failCountValue == null) {
            return;
        }
        try {
            if (Long.parseLong(failCountValue) >= limit) {
                throw new BusinessException(429, message);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void recordLoginFail(String failKey) {
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failCount != null && failCount == 1L) {
            stringRedisTemplate.expire(failKey, lockMinutes, TimeUnit.MINUTES);
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.replace(":", "_").replace(".", "_");
    }

    private String accountFailKeySegment(String username, String usernamePhoneHash) {
        if (usernamePhoneHash != null && !usernamePhoneHash.isBlank()) {
            return "phone:" + usernamePhoneHash;
        }
        return username;
    }

    private String normalizeResetPhone(String phone) {
        String normalizedPhone = privacyProtectionUtil.normalizePhone(phone);
        if (normalizedPhone == null || normalizedPhone.length() != 11) {
            throw new BusinessException(400, "请输入有效的11位手机号");
        }
        return normalizedPhone;
    }

    private void validateNewPassword(String newPassword, String confirmPassword, String phone) {
        String password = newPassword == null ? "" : newPassword.trim();
        if (!Objects.equals(password, confirmPassword == null ? "" : confirmPassword.trim())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        if (password.length() < 8 || password.length() > 64) {
            throw new BusinessException(400, "新密码长度需为8-64位");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException(400, "新密码需同时包含字母和数字");
        }
        if (Objects.equals(password, phone)) {
            throw new BusinessException(400, "新密码不能与手机号相同");
        }
    }

    private LoginUserRow resolvePasswordResetUser(String phone, String phoneHash, String account) {
        String normalizedAccount = normalizeResetAccount(account);
        List<LoginUserRow> users = authMapper.selectLoginUsersByPhoneInTenants(phone, phoneHash, normalizedAccount, allowedTenantCodes());
        if (users == null || users.isEmpty()) {
            throw new BusinessException(404, "该手机号未绑定可用管理端账号");
        }
        List<LoginUserRow> usableUsers = users.stream()
                .filter(user -> user != null && isUsableEmployeeStatus(user.getUserStatus()))
                .toList();
        if (usableUsers.size() > 1) {
            throw new BusinessException(409, "该手机号存在多个可用账号，请输入登录账号后再获取验证码");
        }
        if (usableUsers.isEmpty()) {
            throw new BusinessException(403, "该手机号对应员工账号已离职或停用，请联系管理员重新启用后再重置密码");
        }
        return usableUsers.get(0);
    }

    private String normalizeResetAccount(String account) {
        if (account == null || account.trim().isEmpty()) {
            return null;
        }
        return account.trim();
    }

    private List<String> allowedTenantCodes() {
        return boundedTenantProperties.allowedTenantCodes();
    }

    private LoginUserRow resolveUniqueLoginUser(List<LoginUserRow> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }
        List<LoginUserRow> usableUsers = users.stream()
                .filter(user -> user != null && isUsableEmployeeStatus(user.getUserStatus()))
                .toList();
        if (usableUsers.size() > 1) {
            throw new BusinessException(409, "该账号存在多个组织，请联系管理员保留唯一登录账号或手机号");
        }
        if (usableUsers.size() == 1) {
            return usableUsers.get(0);
        }
        return users.get(0);
    }

    private boolean isUsableEmployeeStatus(Integer status) {
        return Objects.equals(status, EmployeeStatusEnum.ACTIVE.getCode())
                || Objects.equals(status, EmployeeStatusEnum.PROBATION.getCode());
    }

    private String employeeStatusMessage(Integer status) {
        if (Objects.equals(status, EmployeeStatusEnum.RESIGNED.getCode())) {
            return "该员工账号已离职，请联系管理员重新启用";
        }
        return "该员工账号已禁用";
    }

    private String generateSmsCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void ensurePasswordResetNotLocked(String phoneHash) {
        String failCountValue = stringRedisTemplate.opsForValue().get(passwordResetFailKey(phoneHash));
        if (failCountValue == null || failCountValue.isBlank()) {
            return;
        }
        try {
            if (Long.parseLong(failCountValue) >= PASSWORD_RESET_MAX_VERIFY_FAIL) {
                throw new BusinessException(429, "验证码错误次数过多，请重新获取验证码");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void recordPasswordResetFail(String phoneHash) {
        Long failCount = stringRedisTemplate.opsForValue().increment(passwordResetFailKey(phoneHash));
        if (failCount != null && failCount == 1L) {
            stringRedisTemplate.expire(passwordResetFailKey(phoneHash), PASSWORD_RESET_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
    }

    private LoginVO buildLoginVO(LoginUserRow loginUser, String rawPassword) {
        if (rawPassword != null && !encryptUtil.isBcryptHash(loginUser.getPassword())) {
            authMapper.updatePasswordByUserId(loginUser.getUserId(), encryptUtil.encode(rawPassword));
        }

        List<String> permissionList = authMapper.selectPermCodesByUserIdAndTenantCode(loginUser.getUserId(), loginUser.getTenantCode());
        Set<String> permCodes = new LinkedHashSet<>(permissionList == null ? List.of() : permissionList);
        String token = TokenUtil.createToken(loginUser.getUserId(), loginUser.getTenantCode());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(loginUser.getUserId());
        loginVO.setUserName(loginUser.getUserName());
        loginVO.setTenantCode(loginUser.getTenantCode());
        loginVO.setTenantName(loginUser.getTenantName());
        loginVO.setDeveloper(false);
        loginVO.setMustChangePassword(Objects.equals(loginUser.getMustChangePassword(), 1));
        loginVO.setResponseKey(responseEncryptUtil.buildResponseKey(token));
        loginVO.setPermissions(List.copyOf(permCodes));
        loginVO.setFeatures(tenantLicenseService.enabledFeatureKeys(loginUser.getTenantCode()));
        return loginVO;
    }

    private WebScanLoginRedisPayload getWebScanPayload(String sceneKey) {
        String payloadJson = stringRedisTemplate.opsForValue().get(webScanLoginKey(sceneKey));
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, WebScanLoginRedisPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "扫码登录会话解析失败");
        }
    }

    private void saveWebScanPayload(String sceneKey, WebScanLoginRedisPayload payload, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(
                    webScanLoginKey(sceneKey),
                    objectMapper.writeValueAsString(payload),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "扫码登录会话保存失败");
        }
    }

    private String buildQrCodeDataUrl(String content) {
        try {
            var hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 280, 280, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception exception) {
            throw new BusinessException(500, "网页扫码二维码生成失败");
        }
    }

    private String webScanLoginKey(String sceneKey) {
        return redisKeyBuilder.cache("auth", "web-scan-login", sceneKey);
    }

    private String passwordResetCodeKey(String phoneHash) {
        return redisKeyBuilder.cache("auth", "password-reset", "code", phoneHash);
    }

    private String passwordResetSendLockKey(String phoneHash) {
        return redisKeyBuilder.counter("auth", "password-reset", "send-lock", phoneHash);
    }

    private String passwordResetFailKey(String phoneHash) {
        return redisKeyBuilder.counter("auth", "password-reset", "fail", phoneHash);
    }
}
