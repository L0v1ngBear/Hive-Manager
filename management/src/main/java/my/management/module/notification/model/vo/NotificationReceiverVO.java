package my.management.module.notification.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知接收人视图。
 */
@Data
public class NotificationReceiverVO {

    private Long userId;

    private String userName;

    private String departmentName;

    private String positionName;

    private Integer readFlag;

    private LocalDateTime readTime;

    /** 该用户拥有的权限码，多个权限使用英文逗号分隔。 */
    private String permissionCodes;
}
