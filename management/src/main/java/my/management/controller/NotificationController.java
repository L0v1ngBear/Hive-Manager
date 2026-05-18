package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.notification.model.dto.AnnouncementPublishRequest;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.dto.NotificationTaskCloseRequest;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.notification.service.NotificationService;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/announcements")
    public Result<List<NotificationVO>> announcements(@RequestParam(required = false) Integer limit) {
        return Result.success(notificationService.announcements(limit));
    }

    @PostMapping("/announcements")
    @RequirePermission(value = PermissionCodeEnum.CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH, message = "您没有权限发布企业通知")
    @CollectLog(module = "notification", action = "publish_announcement", bizType = "announcement", description = "发布企业通知公告")
    public Result<NotificationVO> publishAnnouncement(@RequestBody AnnouncementPublishRequest request) {
        return Result.success(notificationService.publishAnnouncement(request));
    }

    @PostMapping("/{id}/read")
    @CollectLog(module = "notification", action = "mark_read", bizType = "notification", bizNo = "#id", description = "标记通知已读")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/close")
    @CollectLog(module = "notification", action = "close_task", bizType = "notification", bizNo = "#id", description = "关闭待办任务")
    public Result<Void> close(@PathVariable Long id, @RequestBody NotificationTaskCloseRequest request) {
        notificationService.closeTask(id, request);
        return Result.success(null);
    }

    @PostMapping("/sync-ai")
    @CollectLog(module = "notification", action = "sync_ai", bizType = "ai_advice_notification", description = "同步 AI 建议通知")
    public Result<Integer> syncAi() {
        return Result.success(notificationService.syncAiAdviceNotificationsForCurrentTenant());
    }
}
