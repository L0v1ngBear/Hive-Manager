package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoAttachmentSupportContractTest {

    @Test
    void storageProvidersAndUploadLimitsSupportBusinessVideos() throws IOException {
        String business = read("src/main/java/my/hive/infrastructure/storage/BusinessAttachmentService.java");
        String local = read("src/main/java/my/hive/infrastructure/storage/LocalFileStorageService.java");
        String oss = read("src/main/java/my/hive/infrastructure/storage/OssStorageProperties.java");
        String application = read("src/main/resources/application.yaml");

        for (String extension : new String[]{"mp4", "mov", "m4v", "avi", "mkv", "webm", "3gp"}) {
            assertTrue(business.contains("\"" + extension + "\""), "business attachments must support " + extension);
            assertTrue(local.contains("\"" + extension + "\""), "local storage must support " + extension);
            assertTrue(oss.contains("\"" + extension + "\""), "OSS storage must support " + extension);
        }
        assertTrue(application.contains("APP_UPLOAD_MAX_FILE_SIZE:800MB"));
        assertTrue(application.contains("APP_UPLOAD_MAX_REQUEST_SIZE:820MB"));
        assertTrue(application.contains("APP_UPLOAD_MAX_FILE_SIZE_MB:800"));
        assertTrue(application.contains("ALIYUN_OSS_MAX_FILE_SIZE_MB:800"));
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
