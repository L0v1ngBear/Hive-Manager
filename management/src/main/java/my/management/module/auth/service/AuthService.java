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
import my.hive.common.utils.EncryptUtil;
import my.hive.common.utils.ResponseEncryptUtil;
import my.hive.common.utils.TokenUtil;
import my.management.common.service.DeveloperAccessService;
import my.management.module.auth.mapper.AuthMapper;
import my.management.module.auth.model.dto.LoginRequest;
import my.management.module.auth.model.dto.WebScanConfirmRequest;
import my.management.module.auth.model.vo.LoginUserRow;
import my.management.module.auth.model.vo.LoginVO;
import my.management.module.auth.model.vo.WebScanSessionVO;
import my.management.module.auth.model.vo.WebScanStatusVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

    private static final String WEB_SCAN_LOGIN_KEY_PREFIX = "auth:web-scan-login:";
    private static final long WEB_SCAN_LOGIN_EXPIRE_SECONDS = 180L;

    @Resource
    private AuthMapper authMapper;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private DeveloperAccessService developerAccessService;

    @Resource
    private ResponseEncryptUtil responseEncryptUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    public LoginVO login(LoginRequest request) {
        LoginUserRow loginUser = authMapper.selectLoginUser(request.getUsername().trim());
        validatePasswordLogin(loginUser, request.getPassword());
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

    private void validatePasswordLogin(LoginUserRow loginUser, String password) {
        if (loginUser == null) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!Objects.equals(loginUser.getUserStatus(), 1)) {
            throw new BusinessException(403, "该员工账号已禁用");
        }
        if (password == null || !encryptUtil.matches(password, loginUser.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
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
        loginVO.setDeveloper("super".equalsIgnoreCase(loginUser.getTenantCode()));
        loginVO.setResponseKey(responseEncryptUtil.buildResponseKey(token));
        loginVO.setPermissions(List.copyOf(permCodes));
        return loginVO;
    }

    private WebScanLoginRedisPayload getWebScanPayload(String sceneKey) {
        String payloadJson = stringRedisTemplate.opsForValue().get(WEB_SCAN_LOGIN_KEY_PREFIX + sceneKey);
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
                    WEB_SCAN_LOGIN_KEY_PREFIX + sceneKey,
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
}
