package my.hive.api.wechat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.hive.infrastructure.wechat.WechatSubscribeConfig;
import my.hive.infrastructure.wechat.WechatSubscribeRegisterRequest;
import my.hive.infrastructure.wechat.WechatSubscribeService;
import my.hive.shared.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/subscriptions")
@RequiredArgsConstructor
public class WechatSubscribeController {

    private final WechatSubscribeService service;

    @GetMapping("/config")
    public Result<WechatSubscribeConfig> config() {
        return Result.success(service.config());
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody WechatSubscribeRegisterRequest request) {
        service.register(request);
        return Result.success(null);
    }
}
