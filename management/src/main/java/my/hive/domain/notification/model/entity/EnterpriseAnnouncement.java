package my.hive.domain.notification.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业通知公告主表实体，对应 enterprise_announcement 表。
 */
@Data
public class EnterpriseAnnouncement {

    private Long id;

    private String tenantCode;

    private String announcementCode;

    private String title;

    private String content;

    private String level;

    private String route;

    private Integer status;

    private Long publisherUserId;

    private String publisherName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
