package my.management.module.notification.model.dto;

import lombok.Data;

/**
 * 通知待办关闭入参。
 *
 * <p>AI 建议转成待办后，用户可以把待办标记为已处理或暂不处理。
 * 处理结果会回流到 AI 训练样本，用于后续建议质量评估。</p>
 */
@Data
public class NotificationTaskCloseRequest {

    /**
     * DONE：已处理；IGNORED：暂不处理。
     */
    private String taskStatus;

    /**
     * 处理备注，可为空。
     */
    private String closeNote;
}
