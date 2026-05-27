package my.management.module.notification.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 前端通知展示对象。
 */
@Data
public class NotificationVO {

    private Long id;

    private String title;

    private String content;

    private String level;

    private String type;

    private String channel;

    private String route;

    private Integer readFlag;

    private String taskStatus;

    private String closeResult;

    private String closeNote;

    private String sourceType;

    private Long readCount;

    private Long unreadCount;

    private Long totalReceiverCount;

    private List<NotificationReceiverVO> receivers;

    private LocalDateTime updateTime;
}
