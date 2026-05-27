package my.management.module.notification.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知接收人视图。
 *
 * <p>AI 经营建议属于管理层决策信息，只会推送给拥有 AI 建议权限的用户。</p>
 */
@Data
public class NotificationReceiverVO {

    private Long userId;

    private String userName;

    private String departmentName;

    private String positionName;

    private Integer readFlag;

    private LocalDateTime readTime;

    /** 该用户拥有的 AI 建议权限码，多个权限使用英文逗号分隔。 */
    private String permissionCodes;
}
