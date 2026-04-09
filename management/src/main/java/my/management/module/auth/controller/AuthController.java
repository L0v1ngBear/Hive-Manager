package my.management.module.auth.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.dto.Result;
import my.management.module.auth.model.dto.LoginRequest;
import my.management.module.auth.model.vo.LoginVO;
import my.management.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}