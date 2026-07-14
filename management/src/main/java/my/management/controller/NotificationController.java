package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.management.module.notification.model.dto.AnnouncementPublishRequest;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.dto.NotificationTaskCloseRequest;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.notification.service.EnterpriseAnnouncementService;
import my.management.module.notification.service.NotificationService;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心控制器，承接自动提醒、企业公告和待办闭环入口。
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private EnterpriseAnnouncementService enterpriseAnnouncementService;

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
    @RequirePermission(value = PermissionCatalogV3.CODE_NOTIFICATION_ANNOUNCEMENT_LIST, message = "您没有权限查看企业通知公告")
    public Result<List<NotificationVO>> announcements(@RequestParam(required = false) Integer limit,
                                                      @RequestParam(required = false) String levels) {
        return Result.success(enterpriseAnnouncementService.announcements(limit, levels));
    }

    @PostMapping("/announcements")
    @RequirePermission(value = PermissionCatalogV3.CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH, message = "您没有权限发布企业通知")
    @CollectLog(module = "notification", action = "publish_announcement", bizType = "announcement", description = "发布企业通知公告")
    public Result<NotificationVO> publishAnnouncement(@RequestBody AnnouncementPublishRequest request) {
        return Result.success(enterpriseAnnouncementService.publishAnnouncement(request));
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

    @PostMapping("/sync")
    @RequirePermission(value = PermissionCatalogV3.CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH, message = "您没有权限同步待办通知")
    @CollectLog(module = "notification", action = "sync_all", bizType = "notification", description = "同步待办通知")
    public Result<Integer> syncAll() {
        return Result.success(notificationService.syncAllNotificationsForCurrentTenant());
    }
}
