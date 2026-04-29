package my.management.module.notification.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知记录实体，对应 notification_record 表。
 */
@Data
public class NotificationRecord {

    private Long id;

    private String tenantCode;

    private String dedupeKey;

    private String bizType;

    private String bizId;

    private String title;

    private String content;

    private String level;

    private String channel;

    private String route;

    private Long receiverUserId;

    private String receiverName;

    private String receiverPhone;

    private Integer status;

    private Integer readFlag;

    private LocalDateTime readTime;

    private String sendStatus;

    private LocalDateTime sendTime;

    private String taskStatus;

    private String closeResult;

    private String closeNote;

    private Long closeUserId;

    private LocalDateTime closeTime;

    private String sourceType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
