package my.hive.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.hive.domain.auth.model.WechatLoginRequest;
import my.hive.domain.auth.model.dto.WechatTenantSelectRequest;
import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.dto.Result;
import my.hive.domain.auth.model.dto.LoginRequest;
import my.hive.domain.auth.model.vo.LoginVO;
import my.hive.domain.auth.model.vo.MiniWechatLoginVO;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth/mini")
public class MiniAuthController {
    private final AuthenticationService authentication;
    public MiniAuthController(AuthenticationService authentication) { this.authentication=authentication; }
    @PostMapping("/login")
    @CollectLog(module = "auth", action = "mini_login", bizType = "authentication", description = "小程序账号登录", recordArgs = false)
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) { return Result.success(authentication.miniLogin(r, req.getRemoteAddr())); }
    @PostMapping("/wechat-login")
    @CollectLog(module = "auth", action = "wechat_login", bizType = "authentication", description = "微信小程序登录", recordArgs = false)
    public Result<MiniWechatLoginVO> wechat(@Valid @RequestBody WechatLoginRequest request) {
        return Result.success(authentication.wechatLogin(request));
    }
    @PostMapping("/wechat-login/select")
    @CollectLog(module = "auth", action = "wechat_tenant_select", bizType = "authentication", description = "微信小程序企业选择", recordArgs = false)
    public Result<LoginVO> selectWechatTenant(@Valid @RequestBody WechatTenantSelectRequest request) {
        return Result.success(authentication.selectWechatTenant(request));
    }
}
