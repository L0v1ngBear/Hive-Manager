package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心控制器，承接自动提醒、AI 建议闭环和后续短信推送入口。
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping("/page")
    public Result<PageResult<NotificationVO>> page(NotificationPageRequest request) {
        return Result.success(notificationService.page(request));
    }

    @GetMapping("/unread")
    public Result<List<NotificationVO>> unread() {
        return Result.success(notificationService.unread());
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success(null);
    }

    @PostMapping("/sync-ai")
    public Result<Integer> syncAi() {
        return Result.success(notificationService.syncAiAdviceNotificationsForCurrentTenant());
    }
}
