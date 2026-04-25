package my.management.module.notification.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

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

    private String sourceType;

    private LocalDateTime updateTime;
}
