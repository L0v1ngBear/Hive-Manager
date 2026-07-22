package my.hive.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import my.hive.shared.security.InternalStorageReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OssStorageServiceContractTest {

    @Test
    void productionConstructorIsExplicitlyAutowiredWhileTestConstructorRemainsPackagePrivate() throws Exception {
        assertThat(OssStorageService.class
                .getConstructor(OssStorageProperties.class, ExternalApiGuardService.class)
                .isAnnotationPresent(Autowired.class))
                .isTrue();
        assertThat(OssStorageService.class
                .getDeclaredConstructor(OssStorageProperties.class, ExternalApiGuardService.class, Supplier.class)
                .getModifiers() & java.lang.reflect.Modifier.PUBLIC)
                .isZero();
    }

    @Test
    void uploadBuildsObjectKeyFromSanitizedPrefixTenantModuleAndDate() {
        OssStorageProperties properties = configuredProperties();
        properties.setPathPrefix(" //finance//files?/ ");
        OSS ossClient = mock(OSS.class);
        when(ossClient.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(mock(PutObjectResult.class));
        OssStorageService service = new OssStorageService(properties, mock(ExternalApiGuardService.class), () -> ossClient);

        FileUploadResult result = service.upload(file(), "tenant / 01", "billing/reports");

        String expectedDate = LocalDate.now().toString().replace("-", "/");
        assertThat(result.getObjectKey())
                .matches("finance/files-/tenant---01/billing-reports/" + expectedDate + "/[0-9a-f-]+\\.pdf");
        assertThat(result.getUrl()).startsWith("/storage/private/aliyun-oss/");
        verify(ossClient).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    void tenantLogoUploadReturnsTheOnlyPublicOssReferenceShape() {
        OssStorageProperties properties = configuredProperties();
        OSS ossClient = mock(OSS.class);
        when(ossClient.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(mock(PutObjectResult.class));
        OssStorageService service = new OssStorageService(properties, mock(ExternalApiGuardService.class), () -> ossClient);

        FileUploadResult result = service.upload(file(), "tenant-a", "tenant-logo");

        assertThat(result.getUrl()).startsWith("/storage/public/tenant-logo/");
    }

    @Test
    void disabledConfigurationFailsBeforeConstructingClientAndDoesNotExposeSecret() {
        OssStorageProperties properties = configuredProperties();
        properties.setEnabled(false);
        Supplier<OSS> clientSupplier = mock(Supplier.class);
        OssStorageService service = new OssStorageService(properties, mock(ExternalApiGuardService.class), clientSupplier);

        assertThatThrownBy(() -> service.upload(file(), "tenant-a", "document"))
                .isInstanceOf(BusinessException.class)
                .hasMessageNotContaining(properties.getAccessKeySecret());

        verifyNoInteractions(clientSupplier);
    }

    @Test
    void incompleteConfigurationFailsBeforeConstructingClientAndDoesNotExposeSecret() {
        OssStorageProperties properties = configuredProperties();
        properties.setEndpoint(null);
        Supplier<OSS> clientSupplier = mock(Supplier.class);
        OssStorageService service = new OssStorageService(properties, mock(ExternalApiGuardService.class), clientSupplier);

        assertThatThrownBy(() -> service.upload(file(), "tenant-a", "document"))
                .isInstanceOf(BusinessException.class)
                .hasMessageNotContaining(properties.getAccessKeySecret());

        verifyNoInteractions(clientSupplier);
    }

    @Test
    void loadsPrivateObjectAndClosesOssClientWithTheResource() throws Exception {
        OssStorageProperties properties = configuredProperties();
        OSS ossClient = mock(OSS.class);
        String objectKey = "hive/tenant-a/document/2026/07/22/a.pdf";
        OSSObject ossObject = new OSSObject();
        ossObject.setObjectContent(new ByteArrayInputStream("content".getBytes()));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(7L);
        ossObject.setObjectMetadata(metadata);
        when(ossClient.getObject(properties.getBucketName(), objectKey)).thenReturn(ossObject);
        OssStorageService service = new OssStorageService(properties, mock(ExternalApiGuardService.class), () -> ossClient);

        org.springframework.core.io.Resource resource = service.load(
                InternalStorageReference.privateOssReference("", objectKey),
                "tenant-a",
                "document"
        );

        try (InputStream inputStream = resource.getInputStream()) {
            assertThat(inputStream.readAllBytes()).isEqualTo("content".getBytes());
        }
        verify(ossClient).getObject(properties.getBucketName(), objectKey);
        verify(ossClient).shutdown();
    }

    private OssStorageProperties configuredProperties() {
        OssStorageProperties properties = new OssStorageProperties();
        properties.setEnabled(true);
        properties.setEndpoint("https://oss.example.test");
        properties.setBucketName("test-bucket");
        properties.setAccessKeyId("test-key-id");
        properties.setAccessKeySecret("not-a-real-secret");
        return properties;
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", "content".getBytes());
    }
}
