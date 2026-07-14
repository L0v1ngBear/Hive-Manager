package my.hive.api.auth;

import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.dto.Result;
import my.management.module.auth.model.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth")
public class SessionController {
    private final AuthenticationService authentication;
    public SessionController(AuthenticationService authentication) { this.authentication=authentication; }
    @GetMapping("/me") public Result<LoginVO> me() { return Result.success(authentication.currentUser()); }
    @PostMapping("/logout") public Result<Void> logout() { authentication.logout(); return Result.success(null); }
}
