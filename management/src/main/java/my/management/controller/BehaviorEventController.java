package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.dto.Result;
import my.management.module.behavior.model.dto.BehaviorEventRequest;
import my.management.module.behavior.service.BehaviorEventService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户行为采集入口。
 *
 * <p>该接口只接收可用于 AI 建议进化的业务行为元数据，例如页面访问、建议点击和通知跳转。
 * 禁止上传密码、身份证、完整文本正文等敏感内容。</p>
 */
@RestController
@RequestMapping("/behavior-events")
public class BehaviorEventController {

    @Resource
    private BehaviorEventService behaviorEventService;

    @PostMapping("/batch")
    public Result<Void> batch(@Valid @RequestBody List<BehaviorEventRequest> requests) {
        behaviorEventService.batchSave(requests);
        return Result.success(null);
    }
}
