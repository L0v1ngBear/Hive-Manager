package my.management.common.storage;

import lombok.Data;

/**
 * Generic upload response for business attachments that are stored under /uploads.
 */
@Data
public class BusinessAttachmentVO {

    private String fileName;

    private String fileUrl;

    private Long fileSize;
}
