package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.dto.Result;
import my.management.module.auth.model.dto.InitialPasswordChangeRequest;
import my.management.module.auth.model.dto.LoginRequest;
import my.management.module.auth.model.dto.OrganizationJoinCodeSendRequest;
import my.management.module.auth.model.dto.OrganizationJoinRequest;
import my.management.module.auth.model.dto.PasswordResetCodeRequest;
import my.management.module.auth.model.dto.PasswordResetRequest;
import my.management.module.auth.model.dto.WebScanConfirmRequest;
import my.management.module.auth.model.vo.LoginVO;
import my.management.module.auth.model.vo.WebScanSessionVO;
import my.management.module.auth.model.vo.WebScanStatusVO;
import my.management.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * AuthController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    @CollectLog(module = "auth", action = "login", bizType = "account", description = "管理端账号登录", recordArgs = false, recordResult = false)
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, resolveClientIp(servletRequest)));
    }

    @PostMapping("/password-reset/code")
    @CollectLog(module = "auth", action = "password_reset_code", bizType = "account", description = "发送网页端密码重置验证码", recordArgs = false, recordResult = false)
    public Result<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        authService.sendPasswordResetCode(request);
        return Result.success(null);
    }

    @PostMapping("/password-reset")
    @CollectLog(module = "auth", action = "password_reset", bizType = "account", description = "网页端短信验证码重置密码", recordArgs = false, recordResult = false)
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPasswordBySmsCode(request);
        return Result.success(null);
    }

    @PostMapping("/join-organization/code")
    @CollectLog(module = "auth", action = "organization_join_code", bizType = "account", description = "发送网页登录组织验证码", recordArgs = false, recordResult = false)
    public Result<Void> sendOrganizationJoinCode(@Valid @RequestBody OrganizationJoinCodeSendRequest request) {
        authService.sendOrganizationJoinCode(request);
        return Result.success(null);
    }

    @PostMapping("/join-organization")
    @CollectLog(module = "auth", action = "organization_join", bizType = "account", description = "网页登录加入组织", recordArgs = false, recordResult = false)
    public Result<LoginVO> joinOrganization(@Valid @RequestBody OrganizationJoinRequest request) {
        return Result.success(authService.joinOrganization(request));
    }

    @PostMapping("/initial-password")
    @CollectLog(module = "auth", action = "initial_password_change", bizType = "account", description = "首次登录修改初始密码", recordArgs = false, recordResult = false)
    public Result<Void> changeInitialPassword(@Valid @RequestBody InitialPasswordChangeRequest request) {
        authService.changeInitialPassword(request);
        return Result.success(null);
    }

    @PostMapping("/scan-login/session")
    @CollectLog(module = "auth", action = "scan_login_session", bizType = "web_scan_login", description = "创建网页扫码登录会话", recordArgs = false, recordResult = false)
    public Result<WebScanSessionVO> createScanLoginSession() {
        return Result.success(authService.createWebScanLoginSession());
    }

    @GetMapping("/scan-login/status")
    public Result<WebScanStatusVO> getScanLoginStatus(@RequestParam String sceneKey) {
        return Result.success(authService.getWebScanLoginStatus(sceneKey));
    }

    @PostMapping("/scan-login/confirm")
    @CollectLog(module = "auth", action = "scan_login_confirm", bizType = "web_scan_login", description = "小程序确认网页扫码登录", recordArgs = false, recordResult = false)
    public Result<Void> confirmScanLogin(@Valid @RequestBody WebScanConfirmRequest request) {
        authService.confirmWebScanLogin(request);
        return Result.success(null);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }
}
