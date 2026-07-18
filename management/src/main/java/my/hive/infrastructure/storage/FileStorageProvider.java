package my.hive.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageProvider {

    String providerCode();

    FileUploadResult upload(MultipartFile file, String tenantCode, String module);

    void deleteQuietly(String objectKey);
}
