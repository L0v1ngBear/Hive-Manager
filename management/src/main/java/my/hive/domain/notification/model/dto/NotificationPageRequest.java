package my.hive.domain.notification.model.dto;

import lombok.Data;

/**
 * 通知分页查询入参。
 */
@Data
public class NotificationPageRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private Boolean onlyUnread = false;
}
