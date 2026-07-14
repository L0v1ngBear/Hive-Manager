package my.hive.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.dto.Result;
import my.management.module.auth.model.dto.*;
import my.management.module.auth.model.vo.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/admin")
public class AdminAuthController {
    private final AuthenticationService authentication;
    public AdminAuthController(AuthenticationService authentication) { this.authentication = authentication; }
    @PostMapping("/login") public Result<LoginVO> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) { return Result.success(authentication.adminLogin(r, clientIp(req))); }
    @PostMapping("/scan-login/session") public Result<WebScanSessionVO> scanSession() { return Result.success(authentication.createScanLoginSession()); }
    @GetMapping("/scan-login/status") public Result<WebScanStatusVO> scanStatus(@RequestParam String sceneKey) { return Result.success(authentication.scanLoginStatus(sceneKey)); }
    @PostMapping("/scan-login/confirm") public Result<Void> scanConfirm(@Valid @RequestBody WebScanConfirmRequest r) { authentication.confirmWebScanLogin(r); return Result.success(null); }
    @PostMapping("/password-reset/code") public Result<Void> resetCode(@Valid @RequestBody PasswordResetCodeRequest r) { authentication.sendPasswordResetCode(r); return Result.success(null); }
    @PostMapping("/password-reset") public Result<Void> reset(@Valid @RequestBody PasswordResetRequest r) { authentication.resetPasswordBySmsCode(r); return Result.success(null); }
    @PostMapping("/join-organization/code") public Result<Void> joinCode(@Valid @RequestBody OrganizationJoinCodeSendRequest r) { authentication.sendOrganizationJoinCode(r); return Result.success(null); }
    @PostMapping("/join-organization") public Result<LoginVO> join(@Valid @RequestBody OrganizationJoinRequest r) { return Result.success(authentication.joinOrganization(r)); }
    @PostMapping("/initial-password") public Result<Void> initialPassword(@Valid @RequestBody InitialPasswordChangeRequest r) { authentication.changeInitialPassword(r); return Result.success(null); }
    private String clientIp(HttpServletRequest r) { String f=r.getHeader("X-Forwarded-For"); return f==null||f.isBlank()?r.getRemoteAddr():f.split(",")[0].trim(); }
}
