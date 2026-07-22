package my.hive.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage.oss")
public class OssStorageProperties {

    private boolean enabled = false;

    private String endpoint;

    private String bucketName;

    private String accessKeyId;

    private String accessKeySecret;

    private String publicBaseUrl;

    private String pathPrefix = "hive";

    private long maxFileSizeMb = 20;

    private List<String> allowedExtensions = new ArrayList<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx", "csv",
            "txt", "zip", "rar", "7z", "ppt", "pptx"
    ));

    private List<String> allowedContentTypes = new ArrayList<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/plain",
            "application/zip",
            "application/x-rar-compressed",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ));
}
