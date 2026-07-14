package my.management.module.notification.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.module.notification.mapper.NotificationMapper;
import my.management.module.notification.model.dto.NotificationPageRequest;
import my.management.module.notification.model.dto.NotificationTaskCloseRequest;
import my.management.module.notification.model.entity.NotificationRecord;
import my.management.module.notification.model.enums.NotificationSendStatusEnum;
import my.management.module.notification.model.enums.NotificationTaskStatusEnum;
import my.management.module.notification.model.vo.NotificationReceiverVO;
import my.management.module.notification.model.vo.NotificationVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import my.management.module.inventory.service.InventoryWarningCacheService;
import my.management.module.order.model.vo.OrderWarningSummaryVO;
import my.management.module.order.service.OrderWarningCacheService;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 通知服务，负责把业务预警和流程待办统一沉淀为可跟进的通知。
 */
@Service
public class NotificationService {

    private static final String INVENTORY_WARNING_BIZ_TYPE = "INVENTORY_WARNING";
    private static final String ORDER_STALE_WARNING_BIZ_TYPE = "ORDER_STALE_WARNING";
    private static final String INVENTORY_WARNING_SOURCE = "inventory_warning";
    private static final String ORDER_WARNING_SOURCE = "order_stale_warning";
    private static final int NOTIFICATION_CONTENT_LIMIT = 950;
    private static final int WARNING_NOTIFICATION_LIMIT = 8;
    private static final List<String> INVENTORY_WARNING_PERMISSIONS = List.of(
            PermissionCodeEnum.CODE_INVENTORY_WARNING_LIST, PermissionCodeEnum.CODE_INVENTORY_WARNING_SETTING,
            PermissionCodeEnum.CODE_INVENTORY_RECORD_RECENT, PermissionCodeEnum.CODE_INVENTORY_CLOTH_IN,
            PermissionCodeEnum.CODE_INVENTORY_CLOTH_OUT
    );
    private static final List<String> ORDER_WARNING_PERMISSIONS = List.of(
            PermissionCodeEnum.CODE_ORDER_WARNING_SETTING, PermissionCodeEnum.CODE_ORDER_LIST
    );

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private InventoryWarningCacheService inventoryWarningCacheService;

    @Resource
    private OrderWarningCacheService orderWarningCacheService;

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
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        NotificationRecord record = notificationMapper.selectByIdForUser(tenantCode, userId, id);
        if (record == null) {
            return;
        }
        notificationMapper.markRead(tenantCode, userId, id);
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
        notificationMapper.closeTask(tenantCode, userId, id, taskStatus, closeResult, closeNote);
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncAllNotificationsForCurrentTenant() {
        return syncBusinessWarningNotificationsForCurrentTenant();
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncBusinessWarningNotificationsForCurrentTenant() {
        return syncBusinessWarningNotifications(TenantPermissionContext.getTenantCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncBusinessWarningNotifications(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return 0;
        }
        int count = 0;
        count += syncInventoryWarningNotifications(tenantCode);
        count += syncOrderWarningNotifications(tenantCode);
        return count;
    }

    private int syncInventoryWarningNotifications(String tenantCode) {
        notificationMapper.deactivateActiveBySource(
                tenantCode,
                INVENTORY_WARNING_BIZ_TYPE,
                INVENTORY_WARNING_SOURCE,
                "库存水位已恢复，系统自动关闭预警"
        );
        List<NotificationReceiverVO> receivers = notificationMapper.selectReceiversByPermissions(tenantCode, INVENTORY_WARNING_PERMISSIONS);
        if (receivers == null || receivers.isEmpty()) {
            return 0;
        }
        List<InventoryWarningVO> warnings = inventoryWarningCacheService.topWarnings(tenantCode, WARNING_NOTIFICATION_LIMIT);
        if (warnings == null || warnings.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (InventoryWarningVO warning : warnings) {
            if (warning == null || warning.getModelCode() == null || warning.getModelCode().isBlank()) {
                continue;
            }
            for (NotificationReceiverVO receiver : receivers) {
                count += notificationMapper.upsert(fromInventoryWarning(tenantCode, warning, receiver));
            }
        }
        return count;
    }

    private int syncOrderWarningNotifications(String tenantCode) {
        notificationMapper.deactivateActiveBySource(
                tenantCode,
                ORDER_STALE_WARNING_BIZ_TYPE,
                ORDER_WARNING_SOURCE,
                "订单更新风险已解除，系统自动关闭预警"
        );
        List<NotificationReceiverVO> receivers = notificationMapper.selectReceiversByPermissions(tenantCode, ORDER_WARNING_PERMISSIONS);
        if (receivers == null || receivers.isEmpty()) {
            return 0;
        }
        OrderWarningSummaryVO summary = orderWarningCacheService.summary(tenantCode);
        if (summary == null || summary.getTotalCount() == null || summary.getTotalCount() <= 0) {
            return 0;
        }
        int count = 0;
        for (NotificationReceiverVO receiver : receivers) {
            count += notificationMapper.upsert(fromOrderWarning(tenantCode, summary, receiver));
        }
        return count;
    }

    private NotificationRecord fromInventoryWarning(String tenantCode, InventoryWarningVO warning, NotificationReceiverVO receiver) {
        String modelCode = warning.getModelCode() == null ? "未填写型号" : warning.getModelCode();
        NotificationRecord record = new NotificationRecord();
        record.setTenantCode(tenantCode);
        record.setDedupeKey("INV_WARN:" + sha256(String.join("|", safe(tenantCode), safe(modelCode), safe(receiver.getUserId() == null ? null : String.valueOf(receiver.getUserId())))).substring(0, 48));
        record.setBizType(INVENTORY_WARNING_BIZ_TYPE);
        record.setBizId(modelCode);
        record.setTitle("库存水位预警");
        record.setContent(limit("型号 " + modelCode + " 当前剩余约 " + safe(warning.getTotalMeters() == null ? null : warning.getTotalMeters().stripTrailingZeros().toPlainString()) + " 米，已低于预警水位，请及时核对库存并安排补货。", NOTIFICATION_CONTENT_LIMIT));
        record.setLevel("warning");
        record.setChannel("IN_APP");
        record.setRoute("/function/inventory");
        record.setReceiverUserId(receiver.getUserId());
        record.setReceiverName(receiver.getUserName());
        record.setStatus(CommonStatusEnum.ENABLED.getCode());
        record.setReadFlag(BinaryFlagEnum.NO.getCode());
        record.setSendStatus(NotificationSendStatusEnum.PENDING.getCode());
        record.setTaskStatus(NotificationTaskStatusEnum.PENDING.getCode());
        record.setSourceType(INVENTORY_WARNING_SOURCE);
        return record;
    }

    private NotificationRecord fromOrderWarning(String tenantCode, OrderWarningSummaryVO summary, NotificationReceiverVO receiver) {
        int days = summary.getStaleWarningDays() == null ? 3 : Math.max(summary.getStaleWarningDays(), 1);
        NotificationRecord record = new NotificationRecord();
        record.setTenantCode(tenantCode);
        record.setDedupeKey("ORDER_WARN:" + sha256(String.join("|", safe(tenantCode), String.valueOf(days), safe(receiver.getUserId() == null ? null : String.valueOf(receiver.getUserId())))).substring(0, 48));
        record.setBizType(ORDER_STALE_WARNING_BIZ_TYPE);
        record.setBizId("STALE:" + days);
        record.setTitle("订单未更新预警");
        record.setContent(limit("当前有 " + nvl(summary.getTotalCount()) + " 张订单超过 " + days + " 天未更新，请进入订单管理跟进。", NOTIFICATION_CONTENT_LIMIT));
        record.setLevel("warning");
        record.setChannel("IN_APP");
        record.setRoute("/function/order");
        record.setReceiverUserId(receiver.getUserId());
        record.setReceiverName(receiver.getUserName());
        record.setStatus(CommonStatusEnum.ENABLED.getCode());
        record.setReadFlag(BinaryFlagEnum.NO.getCode());
        record.setSendStatus(NotificationSendStatusEnum.PENDING.getCode());
        record.setTaskStatus(NotificationTaskStatusEnum.PENDING.getCode());
        record.setSourceType(ORDER_WARNING_SOURCE);
        return record;
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

    private String normalizeTaskStatus(String taskStatus) {
        return NotificationTaskStatusEnum.normalizeCloseStatus(taskStatus).getCode();
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
