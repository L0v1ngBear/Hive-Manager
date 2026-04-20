package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.dto.Result;
import my.management.module.auth.model.dto.LoginRequest;
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
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
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
}
