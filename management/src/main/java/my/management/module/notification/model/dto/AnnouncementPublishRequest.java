package my.management.module.notification.model.dto;

import lombok.Data;

/**
 * 企业通知公告发布请求。
 */
@Data
public class AnnouncementPublishRequest {

    private String title;

    private String content;

    private String level;
}
