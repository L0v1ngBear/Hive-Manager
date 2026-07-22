package my.hive.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface FileStorageProvider {

    String providerCode();

    FileUploadResult upload(MultipartFile file, String tenantCode, String module);

    boolean supportsReference(String reference);

    Resource load(String reference, String tenantCode, String module);

    void deleteQuietly(String objectKey);
}
