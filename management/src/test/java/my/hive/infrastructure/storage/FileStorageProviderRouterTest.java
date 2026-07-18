package my.hive.infrastructure.storage;

import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageProviderRouterTest {

    @Test
    void localConfigurationSelectsOnlyTheLocalProvider() {
        RecordingProvider local = new RecordingProvider(" LOCAL ");
        RecordingProvider oss = new RecordingProvider("aliyun-oss");
        FileStorageProviderRouter router = new FileStorageProviderRouter(List.of(local, oss), " local ");

        FileUploadResult result = router.upload(file(), "tenant-a", "document");
        router.deleteQuietly("document/tenant-a/file.pdf");

        assertThat(result.getStorageProvider()).isEqualTo(" LOCAL ");
        assertThat(local.uploadCount).isEqualTo(1);
        assertThat(local.deleteCount).isEqualTo(1);
        assertThat(oss.uploadCount).isZero();
        assertThat(oss.deleteCount).isZero();
    }

    @Test
    void aliyunOssConfigurationSelectsOnlyTheOssProvider() {
        RecordingProvider local = new RecordingProvider("local");
        RecordingProvider oss = new RecordingProvider(" ALIYUN-OSS ");
        FileStorageProviderRouter router = new FileStorageProviderRouter(List.of(local, oss), "aliyun-oss");

        router.upload(file(), "tenant-a", "document");
        router.deleteQuietly("document/tenant-a/file.pdf");

        assertThat(local.uploadCount).isZero();
        assertThat(local.deleteCount).isZero();
        assertThat(oss.uploadCount).isEqualTo(1);
        assertThat(oss.deleteCount).isEqualTo(1);
    }

    @Test
    void duplicateNormalizedProviderCodesAreRejected() {
        assertThatThrownBy(() -> new FileStorageProviderRouter(List.of(
                new RecordingProvider("local"),
                new RecordingProvider(" LOCAL ")
        ), "local"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownConfiguredProviderFailsWithoutFallback() {
        assertThatThrownBy(() -> new FileStorageProviderRouter(List.of(
                new RecordingProvider("local")
        ), "aliyun-oss"))
                .isInstanceOf(BusinessException.class);
    }

    private MultipartFile file() {
        return new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes());
    }

    private static class RecordingProvider implements FileStorageProvider {

        private final String providerCode;
        private int uploadCount;
        private int deleteCount;

        private RecordingProvider(String providerCode) {
            this.providerCode = providerCode;
        }

        @Override
        public String providerCode() {
            return providerCode;
        }

        @Override
        public FileUploadResult upload(MultipartFile file, String tenantCode, String module) {
            uploadCount++;
            return FileUploadResult.builder().storageProvider(providerCode).build();
        }

        @Override
        public void deleteQuietly(String objectKey) {
            deleteCount++;
        }
    }
}
