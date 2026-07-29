package my.hive.domain.notification.model.dto;

import lombok.Data;

/**
 * 企业通知公告发布请求。
 */
@Data
public class AnnouncementPublishRequest {

    private String title;

    private String content;

    private String level;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;
}
