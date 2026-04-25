package my.management.module.notification.service;

import jakarta.annotation.Resource;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.entity.Tenant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 闭环提醒定时任务。
 *
 * <p>它定期把 AI 经营建议和异常预测结果沉淀为通知记录。通知表带去重键，
 * 多实例部署时即使两个后端同时执行，也只会保留一条同业务提醒。</p>
 */
@Component
public class NotificationClosedLoopScheduler {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private NotificationService notificationService;

    @Scheduled(initialDelayString = "${notification.closed-loop.initial-delay:60000}",
            fixedDelayString = "${notification.closed-loop.fixed-delay:900000}")
    public void syncAiAdviceNotifications() {
        List<Tenant> tenants = tenantMapper.selectList(null);
        for (Tenant tenant : tenants) {
            if (tenant == null || tenant.getTenantCode() == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
                continue;
            }
            if (tenant.getStatus() != null && tenant.getStatus() != 1) {
                continue;
            }
            notificationService.syncAiAdviceNotifications(tenant.getTenantCode());
        }
    }
}
