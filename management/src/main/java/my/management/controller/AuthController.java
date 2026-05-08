package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.hive.common.dto.Result;
import my.management.module.auth.model.dto.LoginRequest;
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
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, resolveClientIp(servletRequest)));
    }

    @PostMapping("/password-reset/code")
    public Result<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        authService.sendPasswordResetCode(request);
        return Result.success(null);
    }

    @PostMapping("/password-reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPasswordBySmsCode(request);
        return Result.success(null);
    }

    @PostMapping("/scan-login/session")
    public Result<WebScanSessionVO> createScanLoginSession() {
        return Result.success(authService.createWebScanLoginSession());
    }

    @GetMapping("/scan-login/status")
    public Result<WebScanStatusVO> getScanLoginStatus(@RequestParam String sceneKey) {
        return Result.success(authService.getWebScanLoginStatus(sceneKey));
    }

    @PostMapping("/scan-login/confirm")
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
