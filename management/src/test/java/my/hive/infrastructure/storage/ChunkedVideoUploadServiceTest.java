package my.hive.infrastructure.storage;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChunkedVideoUploadServiceTest {

    @TempDir
    Path uploadRoot;

    private BusinessAttachmentService businessAttachmentService;
    private ChunkedVideoUploadService service;

    @BeforeEach
    void setUp() {
        businessAttachmentService = mock(BusinessAttachmentService.class);
        service = new ChunkedVideoUploadService(businessAttachmentService);
        ReflectionTestUtils.setField(service, "uploadRoot", uploadRoot.toString());
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 800L);
        ReflectionTestUtils.setField(service, "chunkSizeMb", 1);
        ReflectionTestUtils.setField(service, "expireHours", 24);
        TenantPermissionContext.init("TENANT_001", 18L, Set.of());
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void acceptsLargeArchivesUpToEightHundredMegabytes() {
        ChunkedVideoUploadInitRequest request = request("drawing-package.7z", 800L * 1024 * 1024);

        ChunkedVideoUploadInitVO result = service.init("sales-order", request);

        assertThat(result.getUploadId()).isNotBlank();
        assertThat(result.getTotalParts()).isEqualTo(800);
        assertThat(result.getChunkSize()).isEqualTo(1024 * 1024);
    }

    @Test
    void rejectsArchivesAboveTheConfiguredLimit() {
        ChunkedVideoUploadInitRequest request = request("drawing-package.zip", 801L * 1024 * 1024);

        assertThatThrownBy(() -> service.init("sales-order", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("800MB");
    }

    @Test
    void mergesArchivePartsThroughAFileBackedUpload() throws Exception {
        byte[] first = new byte[1024 * 1024];
        java.util.Arrays.fill(first, (byte) 7);
        byte[] second = "tail".getBytes();
        ChunkedVideoUploadInitRequest request = request("drawing-package.zip", first.length + second.length);
        ChunkedVideoUploadInitVO init = service.init("sales-order", request);
        service.uploadPart("sales-order", init.getUploadId(), 1,
                new MockMultipartFile("file", "part-1", "application/octet-stream", first));
        service.uploadPart("sales-order", init.getUploadId(), 2,
                new MockMultipartFile("file", "part-2", "application/octet-stream", second));
        BusinessAttachmentVO expected = new BusinessAttachmentVO();
        expected.setFileName("drawing-package.zip");
        when(businessAttachmentService.upload(any(), eq("sales-order"))).thenAnswer(invocation -> {
            FileBackedMultipartFile merged = invocation.getArgument(0);
            assertThat(merged.getSize()).isEqualTo(first.length + second.length);
            byte[] mergedBytes = merged.getInputStream().readAllBytes();
            assertThat(mergedBytes[0]).isEqualTo((byte) 7);
            assertThat(java.util.Arrays.copyOfRange(mergedBytes, first.length, mergedBytes.length))
                    .isEqualTo(second);
            return expected;
        });

        BusinessAttachmentVO result = service.complete("sales-order", init.getUploadId());

        assertThat(result).isSameAs(expected);
    }

    private ChunkedVideoUploadInitRequest request(String fileName, long fileSize) {
        ChunkedVideoUploadInitRequest request = new ChunkedVideoUploadInitRequest();
        request.setFileName(fileName);
        request.setContentType("application/octet-stream");
        request.setFileSize(fileSize);
        return request;
    }
}
