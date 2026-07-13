package my.management.module.notification.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.module.notification.mapper.EnterpriseAnnouncementMapper;
import my.management.module.notification.model.dto.AnnouncementPublishRequest;
import my.management.module.notification.model.entity.EnterpriseAnnouncement;
import my.management.module.notification.model.vo.NotificationReceiverVO;
import my.management.module.notification.model.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 企业公告服务，公告主数据独立于 notification_record。
 */
@Service
public class EnterpriseAnnouncementService {

    private static final String ANNOUNCEMENT_TYPE = "ANNOUNCEMENT";
    private static final String ANNOUNCEMENT_SOURCE = "manual_announcement";
    private static final String ANNOUNCEMENT_ROUTE = "/dashboard";
    private static final int ANNOUNCEMENT_TITLE_LIMIT = 80;
    private static final int ANNOUNCEMENT_CONTENT_LIMIT = 1000;
    private static final Set<String> ANNOUNCEMENT_LEVELS = Set.of("normal", "urgent", "important");

    @Resource
    private EnterpriseAnnouncementMapper enterpriseAnnouncementMapper;

    public List<NotificationVO> announcements(Integer limit, String levels) {
        int safeLimit = Math.min(Math.max(limit == null ? 5 : limit, 1), 50);
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<String> levelFilters = parseAnnouncementLevels(levels);
        return enterpriseAnnouncementMapper.selectRecentAnnouncements(tenantCode, levelFilters, safeLimit)
                .stream()
                .peek(this::markReadForCurrentUser)
                .map(this::toVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationVO publishAnnouncement(AnnouncementPublishRequest request) {
        String title = normalizeText(request == null ? null : request.getTitle());
        String content = normalizeText(request == null ? null : request.getContent());
        if (title == null) {
            throw new BusinessException("请填写通知标题");
        }
        if (content == null) {
            throw new BusinessException("请填写通知内容");
        }

        EnterpriseAnnouncement announcement = new EnterpriseAnnouncement();
        announcement.setTenantCode(TenantPermissionContext.getTenantCode());
        announcement.setAnnouncementCode("ANNOUNCEMENT:" + UUID.randomUUID());
        announcement.setTitle(limit(title, ANNOUNCEMENT_TITLE_LIMIT));
        announcement.setContent(limit(content, ANNOUNCEMENT_CONTENT_LIMIT));
        announcement.setLevel(normalizeAnnouncementLevel(request == null ? null : request.getLevel()));
        announcement.setRoute(ANNOUNCEMENT_ROUTE);
        announcement.setStatus(CommonStatusEnum.ENABLED.getCode());
        announcement.setPublisherUserId(TenantPermissionContext.getUserId());
        enterpriseAnnouncementMapper.insertAnnouncement(announcement);
        createReceiverRows(announcement);
        return toVO(announcement);
    }

    private void createReceiverRows(EnterpriseAnnouncement announcement) {
        List<NotificationReceiverVO> receivers = enterpriseAnnouncementMapper.selectAnnouncementTargetUsers(announcement.getTenantCode());
        if (receivers == null || receivers.isEmpty()) {
            return;
        }
        for (NotificationReceiverVO receiver : receivers) {
            if (receiver == null || receiver.getUserId() == null) {
                continue;
            }
            enterpriseAnnouncementMapper.upsertReceiver(
                    announcement.getTenantCode(),
                    announcement.getId(),
                    announcement.getAnnouncementCode(),
                    receiver,
                    BinaryFlagEnum.NO.getCode()
            );
        }
    }

    private void markReadForCurrentUser(EnterpriseAnnouncement announcement) {
        Long userId = TenantPermissionContext.getUserId();
        if (announcement == null || announcement.getId() == null || userId == null) {
            return;
        }
        int updated = enterpriseAnnouncementMapper.markRead(announcement.getTenantCode(), announcement.getId(), userId);
        if (updated > 0) {
            return;
        }
        NotificationReceiverVO receiver = new NotificationReceiverVO();
        receiver.setUserId(userId);
        enterpriseAnnouncementMapper.upsertReceiver(
                announcement.getTenantCode(),
                announcement.getId(),
                announcement.getAnnouncementCode(),
                receiver,
                BinaryFlagEnum.YES.getCode()
        );
    }

    private NotificationVO toVO(EnterpriseAnnouncement announcement) {
        NotificationVO vo = new NotificationVO();
        vo.setId(announcement.getId());
        vo.setTitle(announcement.getTitle());
        vo.setContent(announcement.getContent());
        vo.setLevel(announcement.getLevel());
        vo.setType(ANNOUNCEMENT_TYPE);
        vo.setChannel("IN_APP");
        vo.setRoute(announcement.getRoute());
        vo.setSourceType(ANNOUNCEMENT_SOURCE);
        vo.setUpdateTime(announcement.getUpdateTime() == null ? announcement.getCreateTime() : announcement.getUpdateTime());

        List<NotificationReceiverVO> receivers = announcement.getId() == null
                ? List.of()
                : enterpriseAnnouncementMapper.selectReceiverStatuses(announcement.getTenantCode(), announcement.getId());
        long readCount = receivers.stream().filter(receiver -> BinaryFlagEnum.YES.getCode().equals(receiver.getReadFlag())).count();
        vo.setReceivers(receivers);
        vo.setReadCount(readCount);
        vo.setTotalReceiverCount((long) receivers.size());
        vo.setUnreadCount(Math.max(receivers.size() - readCount, 0));
        Long currentUserId = TenantPermissionContext.getUserId();
        vo.setReadFlag(readCurrentUser(receivers, currentUserId) ? BinaryFlagEnum.YES.getCode() : BinaryFlagEnum.NO.getCode());
        return vo;
    }

    private boolean readCurrentUser(List<NotificationReceiverVO> receivers, Long currentUserId) {
        if (currentUserId == null || receivers == null || receivers.isEmpty()) {
            return false;
        }
        return receivers.stream()
                .anyMatch(receiver -> currentUserId.equals(receiver.getUserId())
                        && BinaryFlagEnum.YES.getCode().equals(receiver.getReadFlag()));
    }

    private String normalizeAnnouncementLevel(String level) {
        String text = normalizeText(level);
        if ("urgent".equalsIgnoreCase(text) || "critical".equalsIgnoreCase(text)) {
            return "urgent";
        }
        if ("important".equalsIgnoreCase(text) || "warning".equalsIgnoreCase(text)) {
            return "important";
        }
        return "normal";
    }

    private List<String> parseAnnouncementLevels(String levels) {
        String text = normalizeText(levels);
        if (text == null) {
            return List.of();
        }
        return List.of(text.split(","))
                .stream()
                .map(this::normalizeAnnouncementLevel)
                .filter(ANNOUNCEMENT_LEVELS::contains)
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
