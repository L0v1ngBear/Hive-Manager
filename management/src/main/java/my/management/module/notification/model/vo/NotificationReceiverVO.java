package my.management.module.notification.model.vo;

import lombok.Data;

/**
 * 通知接收人视图。
 *
 * <p>AI 经营建议属于管理层决策信息，只会推送给拥有 AI 建议权限的用户。</p>
 */
@Data
public class NotificationReceiverVO {

    private Long userId;

    private String userName;

    /** 该用户拥有的 AI 建议权限码，多个权限使用英文逗号分隔。 */
    private String permissionCodes;
}
