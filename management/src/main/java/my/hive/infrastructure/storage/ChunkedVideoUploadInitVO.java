package my.hive.infrastructure.storage;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkedVideoUploadInitVO {
    private String uploadId;
    private int chunkSize;
    private int totalParts;
    private List<Integer> uploadedParts;
}
