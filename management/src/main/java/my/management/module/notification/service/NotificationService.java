package my.management.module.notification.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.service.AiAnalysisService;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.notification.mapper.NotificationMapper;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.entity.NotificationRecord;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.notification.sms.SmsMessage;
import my.management.module.notification.sms.SmsSender;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 通知服务，负责把异常预测、AI 建议和流程待办统一沉淀为可跟进的通知。
 */
@Service
public class NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private SmsSender smsSender;

    public PageResult<NotificationVO> page(NotificationPageRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        int pageNum = Math.max(request.getPageNum() == null ? 1 : request.getPageNum(), 1);
        int pageSize = Math.min(Math.max(request.getPageSize() == null ? 20 : request.getPageSize(), 1), 100);
        int offset = (pageNum - 1) * pageSize;

        List<NotificationVO> records = notificationMapper
                .selectPage(tenantCode, userId, Boolean.TRUE.equals(request.getOnlyUnread()), offset, pageSize)
                .stream()
                .map(this::toVO)
                .toList();
        long total = nvl(notificationMapper.countPage(tenantCode, userId, Boolean.TRUE.equals(request.getOnlyUnread())));

        PageResult<NotificationVO> result = new PageResult<>();
        result.setCurrent((long) pageNum);
        result.setSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setData(records);
        return result;
    }

    public List<NotificationVO> unread() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        return notificationMapper.selectUnread(tenantCode, userId, 10)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public long unreadCount() {
        return nvl(notificationMapper.countUnread(TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId()));
    }

    public void markRead(Long id) {
        if (id == null) {
            return;
        }
        notificationMapper.markRead(TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId(), id);
    }

    public int syncAiAdviceNotificationsForCurrentTenant() {
        return syncAiAdviceNotifications(TenantPermissionContext.getTenantCode());
    }

    public int syncAiAdviceNotifications(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return 0;
        }
        List<DashboardAiAdviceVO> advices = aiAnalysisService.buildAllDashboardAdvices(tenantCode, fullVisibility());
        int count = 0;
        for (DashboardAiAdviceVO advice : advices) {
            if (!shouldCreateNotification(advice)) {
                continue;
            }
            NotificationRecord record = fromAdvice(tenantCode, advice);
            count += notificationMapper.upsert(record);
            sendSmsIfNeeded(record);
        }
        return count;
    }

    private DashboardOverviewVO.Visibility fullVisibility() {
        DashboardOverviewVO.Visibility visibility = new DashboardOverviewVO.Visibility();
        visibility.setOrderVisible(true);
        visibility.setInventoryVisible(true);
        visibility.setApprovalVisible(true);
        visibility.setReceiptVisible(true);
        visibility.setTrendVisible(true);
        visibility.setAttendanceVisible(true);
        return visibility;
    }

    private boolean shouldCreateNotification(DashboardAiAdviceVO advice) {
        return advice != null && !"success".equals(advice.getLevel()) && advice.getTitle() != null && advice.getSummary() != null;
    }

    private NotificationRecord fromAdvice(String tenantCode, DashboardAiAdviceVO advice) {
        NotificationRecord record = new NotificationRecord();
        record.setTenantCode(tenantCode);
        record.setDedupeKey(buildDedupeKey(advice));
        record.setBizType("AI_ADVICE");
        record.setBizId(advice.getCategory());
        record.setTitle(advice.getTitle());
        record.setContent(buildAdviceContent(advice));
        record.setLevel(resolveLevel(advice.getLevel(), advice.getPriority()));
        record.setChannel("IN_APP");
        record.setRoute(advice.getRoute());
        record.setStatus(1);
        record.setReadFlag(0);
        record.setSendStatus("PENDING");
        record.setSourceType(advice.getSourceType() == null ? "local_rules" : advice.getSourceType());
        return record;
    }

    private String buildAdviceContent(DashboardAiAdviceVO advice) {
        StringBuilder builder = new StringBuilder();
        builder.append(advice.getSummary());
        if (advice.getSuggestion() != null && !advice.getSuggestion().isBlank()) {
            builder.append(" 建议：").append(advice.getSuggestion());
        }
        if (advice.getTrackingHint() != null && !advice.getTrackingHint().isBlank()) {
            builder.append(" 闭环：").append(advice.getTrackingHint());
        }
        return builder.toString();
    }

    private String resolveLevel(String level, String priority) {
        if ("P0".equalsIgnoreCase(priority) || "critical".equalsIgnoreCase(level)) {
            return "critical";
        }
        if ("warning".equalsIgnoreCase(level) || "P1".equalsIgnoreCase(priority)) {
            return "warning";
        }
        return "info";
    }

    private String buildDedupeKey(DashboardAiAdviceVO advice) {
        String raw = String.join("|",
                "AI_ADVICE",
                safe(advice.getCategory()),
                safe(advice.getTitle()),
                safe(advice.getRoute())
        );
        return "AI:" + sha256(raw).substring(0, 48);
    }

    private void sendSmsIfNeeded(NotificationRecord record) {
        if (record.getReceiverPhone() == null || record.getReceiverPhone().isBlank()) {
            return;
        }
        smsSender.send(new SmsMessage(record.getReceiverPhone(), record.getTitle(), record.getContent()));
    }

    private NotificationVO toVO(NotificationRecord record) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(record, vo);
        vo.setType(record.getBizType());
        return vo;
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
