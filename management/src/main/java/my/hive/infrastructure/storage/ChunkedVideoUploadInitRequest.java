package my.hive.infrastructure.storage;

import lombok.Data;

@Data
public class ChunkedVideoUploadInitRequest {
    private String uploadId;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
