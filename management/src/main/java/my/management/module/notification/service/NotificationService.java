package my.management.module.notification.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.module.ai.service.AiAdviceFeedbackService;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.service.AiAdvicePermissionService;
import my.management.module.ai.service.AiAnalysisService;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.notification.mapper.NotificationMapper;
import my.management.module.notification.model.dto.AnnouncementPublishRequest;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.dto.NotificationTaskCloseRequest;
import my.management.module.notification.model.entity.NotificationRecord;
import my.management.module.notification.model.enums.NotificationSendStatusEnum;
import my.management.module.notification.model.enums.NotificationTaskStatusEnum;
import my.management.module.notification.model.vo.NotificationReceiverVO;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.notification.sms.SmsMessage;
import my.management.module.notification.sms.SmsSender;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 通知服务，负责把异常预测、AI 建议和流程待办统一沉淀为可跟进的通知。
 */
@Service
public class NotificationService {

    private static final String AI_ADVICE_BIZ_TYPE = "AI_ADVICE";
    private static final String ANNOUNCEMENT_BIZ_TYPE = "ANNOUNCEMENT";
    private static final int NOTIFICATION_CONTENT_LIMIT = 950;
    private static final int NOTIFICATION_LIST_ITEM_LIMIT = 3;
    private static final int NOTIFICATION_ITEM_TEXT_LIMIT = 90;
    private static final int ANNOUNCEMENT_TITLE_LIMIT = 80;
    private static final int ANNOUNCEMENT_CONTENT_LIMIT = 1000;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private AiAdviceFeedbackService aiAdviceFeedbackService;

    @Resource
    private AiAdvicePermissionService aiAdvicePermissionService;

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

    public List<NotificationVO> announcements(Integer limit) {
        int safeLimit = Math.min(Math.max(limit == null ? 5 : limit, 1), 50);
        return notificationMapper.selectRecentAnnouncements(TenantPermissionContext.getTenantCode(), safeLimit)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public NotificationVO publishAnnouncement(AnnouncementPublishRequest request) {
        String title = normalizeText(request == null ? null : request.getTitle());
        String content = normalizeText(request == null ? null : request.getContent());
        if (title == null) {
            throw new BusinessException("请填写通知标题");
        }
        if (content == null) {
            throw new BusinessException("请填写通知内容");
        }

        NotificationRecord record = new NotificationRecord();
        record.setTenantCode(TenantPermissionContext.getTenantCode());
        record.setDedupeKey("ANNOUNCEMENT:" + UUID.randomUUID());
        record.setBizType(ANNOUNCEMENT_BIZ_TYPE);
        record.setBizId(record.getDedupeKey());
        record.setTitle(limit(title, ANNOUNCEMENT_TITLE_LIMIT));
        record.setContent(limit(content, ANNOUNCEMENT_CONTENT_LIMIT));
        record.setLevel(normalizeAnnouncementLevel(request == null ? null : request.getLevel()));
        record.setChannel("IN_APP");
        record.setRoute("/dashboard");
        record.setStatus(CommonStatusEnum.ENABLED.getCode());
        record.setReadFlag(BinaryFlagEnum.NO.getCode());
        record.setSendStatus(NotificationSendStatusEnum.PENDING.getCode());
        record.setTaskStatus(NotificationTaskStatusEnum.DONE.getCode());
        record.setSourceType("manual_announcement");
        notificationMapper.insertAnnouncement(record);
        return toVO(record);
    }

    public void markRead(Long id) {
        if (id == null) {
            return;
        }
        notificationMapper.markRead(TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId(), id);
    }

    public void closeTask(Long id, NotificationTaskCloseRequest request) {
        if (id == null) {
            return;
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        NotificationRecord record = notificationMapper.selectByIdForUser(tenantCode, userId, id);
        if (record == null) {
            return;
        }

        String taskStatus = normalizeTaskStatus(request == null ? null : request.getTaskStatus());
        String closeNote = limit(request == null ? null : request.getCloseNote(), 500);
        String closeResult = NotificationTaskStatusEnum.DONE.matches(taskStatus) ? "resolved" : "ignored";
        int updated = notificationMapper.closeTask(tenantCode, userId, id, taskStatus, closeResult, closeNote);
        if (updated > 0 && AI_ADVICE_BIZ_TYPE.equals(record.getBizType()) && record.getBizId() != null && record.getBizId().startsWith("AI_SAMPLE:")) {
            try {
                aiAdviceFeedbackService.feedbackBySampleKey(
                        tenantCode,
                        userId,
                        record.getBizId(),
                        closeResult,
                        closeNote
                );
            } catch (BusinessException ignored) {
                // 用户权限调整后仍允许关闭旧通知，AI 反馈写入失败不应阻断待办闭环。
            }
        }
    }

    public int syncAiAdviceNotificationsForCurrentTenant() {
        if (!aiAdvicePermissionService.canViewAny()) {
            return 0;
        }
        return syncAiAdviceNotifications(TenantPermissionContext.getTenantCode());
    }

    public int syncAiAdviceNotifications(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return 0;
        }
        List<NotificationReceiverVO> receivers = notificationMapper.selectAiAdviceReceivers(tenantCode);
        if (receivers == null || receivers.isEmpty()) {
            return 0;
        }
        List<DashboardAiAdviceVO> advices = aiAnalysisService.buildAllDashboardAdvices(tenantCode, fullVisibility());
        int count = 0;
        for (DashboardAiAdviceVO advice : advices) {
            if (!shouldCreateNotification(advice)) {
                continue;
            }
            for (NotificationReceiverVO receiver : receivers) {
                Set<String> permissionCodes = aiAdvicePermissionService.parsePermissionCodes(receiver.getPermissionCodes());
                if (!aiAdvicePermissionService.canViewCategory(advice.getCategory(), permissionCodes)) {
                    continue;
                }
                NotificationRecord record = fromAdvice(tenantCode, advice, receiver);
                count += notificationMapper.upsert(record);
                sendSmsIfNeeded(record);
            }
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
        visibility.setAiAdviceVisible(true);
        return visibility;
    }

    private boolean shouldCreateNotification(DashboardAiAdviceVO advice) {
        if (advice == null || advice.getTitle() == null || advice.getSummary() == null || "success".equals(advice.getLevel())) {
            return false;
        }
        return "P0".equals(advice.getPriority()) || "P1".equals(advice.getPriority());
    }

    private NotificationRecord fromAdvice(String tenantCode, DashboardAiAdviceVO advice, NotificationReceiverVO receiver) {
        NotificationRecord record = new NotificationRecord();
        record.setTenantCode(tenantCode);
        record.setDedupeKey(buildDedupeKey(advice, receiver == null ? null : receiver.getUserId()));
        record.setBizType(AI_ADVICE_BIZ_TYPE);
        record.setBizId(advice.getSampleKey() == null ? advice.getCategory() : advice.getSampleKey());
        record.setTitle(advice.getTitle());
        record.setContent(buildAdviceContent(advice));
        record.setLevel(resolveLevel(advice.getLevel(), advice.getPriority()));
        record.setChannel("IN_APP");
        record.setRoute(advice.getRoute());
        if (receiver != null) {
            record.setReceiverUserId(receiver.getUserId());
            record.setReceiverName(receiver.getUserName());
        }
        record.setStatus(CommonStatusEnum.ENABLED.getCode());
        record.setReadFlag(BinaryFlagEnum.NO.getCode());
        record.setSendStatus(NotificationSendStatusEnum.PENDING.getCode());
        record.setTaskStatus(NotificationTaskStatusEnum.PENDING.getCode());
        record.setSourceType(advice.getSourceType() == null ? "transformer" : advice.getSourceType());
        return record;
    }

    private String buildAdviceContent(DashboardAiAdviceVO advice) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "概况", advice.getSummary());
        appendSection(builder, "建议", advice.getSuggestion());
        appendSection(builder, "预期结果", advice.getExpectedOutcome());
        appendSection(builder, "第一步", advice.getFirstAction());
        appendListSection(builder, "执行清单", advice.getActionSteps());
        appendListSection(builder, "验收标准", advice.getSuccessCriteria());
        appendSection(builder, "复盘时间", advice.getReviewDeadline());
        appendSection(builder, "风控护栏", advice.getRiskGuardrail());
        appendListSection(builder, "数据核验", advice.getDataCheckpoints());
        appendSection(builder, "闭环", advice.getTrackingHint());
        return limit(builder.toString(), NOTIFICATION_CONTENT_LIMIT);
    }

    private void appendSection(StringBuilder builder, String label, String value) {
        String text = normalizeText(value);
        if (text == null) {
            return;
        }
        appendSeparator(builder);
        builder.append(label).append("：").append(text);
    }

    private void appendListSection(StringBuilder builder, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> items = values.stream()
                .map(this::normalizeText)
                .filter(value -> value != null && !value.isBlank())
                .limit(NOTIFICATION_LIST_ITEM_LIMIT)
                .map(value -> limit(value, NOTIFICATION_ITEM_TEXT_LIMIT))
                .toList();
        if (items.isEmpty()) {
            return;
        }
        appendSeparator(builder);
        builder.append(label).append("：").append(String.join("；", items));
    }

    private void appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append(" ");
        }
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

    private String buildDedupeKey(DashboardAiAdviceVO advice, Long receiverUserId) {
        String raw = String.join("|",
                "AI_ADVICE",
                safe(receiverUserId == null ? null : String.valueOf(receiverUserId)),
                safe(advice.getSampleKey()),
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

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeTaskStatus(String taskStatus) {
        return NotificationTaskStatusEnum.normalizeCloseStatus(taskStatus).getCode();
    }

    private String normalizeAnnouncementLevel(String level) {
        String text = normalizeText(level);
        if ("critical".equalsIgnoreCase(text)) {
            return "critical";
        }
        if ("warning".equalsIgnoreCase(text)) {
            return "warning";
        }
        return "info";
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
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
