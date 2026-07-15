package my.hive.domain.notification.model.dto;

import lombok.Data;

/**
 * Notification task close request.
 */
@Data
public class NotificationTaskCloseRequest {

    /**
     * DONE: processed; IGNORED: skipped.
     */
    private String taskStatus;

    /**
     * Optional processing note.
     */
    private String closeNote;
}
