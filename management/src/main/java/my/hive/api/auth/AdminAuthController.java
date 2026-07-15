package my.hive.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.dto.Result;
import my.hive.domain.auth.model.dto.*;
import my.hive.domain.auth.model.vo.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/admin")
public class AdminAuthController {
    private final AuthenticationService authentication;
    public AdminAuthController(AuthenticationService authentication) { this.authentication = authentication; }
    @PostMapping("/login")
    @CollectLog(module = "auth", action = "login", bizType = "authentication", description = "管理端登录", recordArgs = false)
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) { return Result.success(authentication.adminLogin(r, clientIp(req))); }
    @PostMapping("/scan-login/session")
    @CollectLog(module = "auth", action = "scan_login_session", bizType = "authentication", description = "创建扫码登录会话", recordArgs = false)
    public Result<WebScanSessionVO> scanSession() { return Result.success(authentication.createScanLoginSession()); }
    @GetMapping("/scan-login/status") public Result<WebScanStatusVO> scanStatus(@RequestParam String sceneKey) { return Result.success(authentication.scanLoginStatus(sceneKey)); }
    @PostMapping("/scan-login/confirm")
    @CollectLog(module = "auth", action = "scan_login_confirm", bizType = "authentication", description = "确认扫码登录", recordArgs = false)
    public Result<Void> scanConfirm(@Valid @RequestBody WebScanConfirmRequest r) { authentication.confirmWebScanLogin(r); return Result.success(null); }
    @PostMapping("/password-reset/code")
    @CollectLog(module = "auth", action = "password_reset_code", bizType = "authentication", description = "发送密码重置验证码", recordArgs = false)
    public Result<Void> resetCode(@Valid @RequestBody PasswordResetCodeRequest r) { authentication.sendPasswordResetCode(r); return Result.success(null); }
    @PostMapping("/password-reset")
    @CollectLog(module = "auth", action = "password_reset", bizType = "authentication", description = "重置管理端密码", recordArgs = false)
    public Result<Void> reset(@Valid @RequestBody PasswordResetRequest r) { authentication.resetPasswordBySmsCode(r); return Result.success(null); }
    @PostMapping("/join-organization/code")
    @CollectLog(module = "auth", action = "join_organization_code", bizType = "authentication", description = "发送加入组织验证码", recordArgs = false)
    public Result<Void> joinCode(@Valid @RequestBody OrganizationJoinCodeSendRequest r) { authentication.sendOrganizationJoinCode(r); return Result.success(null); }
    @PostMapping("/join-organization")
    @CollectLog(module = "auth", action = "join_organization", bizType = "authentication", description = "加入组织", recordArgs = false)
    public Result<LoginVO> join(@Valid @RequestBody OrganizationJoinRequest r) { return Result.success(authentication.joinOrganization(r)); }
    @PostMapping("/initial-password")
    @CollectLog(module = "auth", action = "initial_password_change", bizType = "authentication", description = "修改初始密码", recordArgs = false)
    public Result<Void> initialPassword(@Valid @RequestBody InitialPasswordChangeRequest r) { authentication.changeInitialPassword(r); return Result.success(null); }
    private String clientIp(HttpServletRequest r) { String f=r.getHeader("X-Forwarded-For"); return f==null||f.isBlank()?r.getRemoteAddr():f.split(",")[0].trim(); }
}
