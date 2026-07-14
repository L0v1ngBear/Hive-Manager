package my.management.module.notification.service;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.event.SystemEvent;
import my.hive.shared.event.SystemEventPublisher;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.entity.Tenant;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Closed-loop notification sync job.
 *
 * <p>The trigger is managed by XXL-JOB instead of Spring Scheduled so it can be
 * enabled, disabled, retried, and audited from the scheduling console.</p>
 */
@Slf4j
@Component
public class NotificationClosedLoopScheduler {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private NotificationService notificationService;

    @Resource
    private SystemEventPublisher systemEventPublisher;

    @XxlJob("notificationClosedLoopJob")
    public void syncClosedLoopNotifications() {
        int syncedTenantCount = 0;
        int failedTenantCount = 0;
        try {
            List<Tenant> tenants = tenantMapper.selectList(null);
            if (tenants == null || tenants.isEmpty()) {
                log.info("notification closed loop sync finished: no tenants");
                XxlJobHelper.log("notification closed loop sync finished: no tenants");
                return;
            }

            for (Tenant tenant : tenants) {
                if (tenant == null || tenant.getTenantCode() == null || DeleteFlagEnum.isDeleted(tenant.getDeleted())) {
                    continue;
                }
                if (tenant.getStatus() != null && !CommonStatusEnum.isEnabled(tenant.getStatus())) {
                    continue;
                }
                try {
                    notificationService.syncBusinessWarningNotifications(tenant.getTenantCode());
                    syncedTenantCount++;
                } catch (Exception ex) {
                    failedTenantCount++;
                    log.error("notification closed loop tenant sync failed, tenantCode={}", tenant.getTenantCode(), ex);
                    XxlJobHelper.log("tenant sync failed, tenantCode={}, error={}", tenant.getTenantCode(), ex.getMessage());
                }
            }

            Map<String, Object> detail = Map.of(
                    "syncedTenantCount", syncedTenantCount,
                    "failedTenantCount", failedTenantCount
            );
            systemEventPublisher.publish(SystemEvent.builder()
                    .eventType("NOTIFICATION_CLOSED_LOOP_SYNC")
                    .level(failedTenantCount > 0 ? "WARN" : "INFO")
                    .module("notification")
                    .title(failedTenantCount > 0 ? "通知闭环同步存在失败租户" : "通知闭环同步完成")
                    .content("成功租户数: " + syncedTenantCount + ", 失败租户数: " + failedTenantCount)
                    .bizType("xxl-job")
                    .bizNo("notificationClosedLoopJob")
                    .detail(detail)
                    .build());
            log.info("notification closed loop sync finished, syncedTenantCount={}, failedTenantCount={}",
                    syncedTenantCount, failedTenantCount);
            XxlJobHelper.log("notification closed loop sync finished, syncedTenantCount={}, failedTenantCount={}",
                    syncedTenantCount, failedTenantCount);
        } catch (Exception ex) {
            log.error("notification closed loop sync job failed", ex);
            XxlJobHelper.log("notification closed loop sync job failed: {}", ex.getMessage());
            systemEventPublisher.error("NOTIFICATION_CLOSED_LOOP_SYNC_FAILED", "通知闭环同步任务失败", ex,
                    Map.of("job", "notificationClosedLoopJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }
}
