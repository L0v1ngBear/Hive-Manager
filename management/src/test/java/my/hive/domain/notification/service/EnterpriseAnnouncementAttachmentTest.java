package my.hive.domain.notification.service;

import my.hive.domain.notification.mapper.EnterpriseAnnouncementMapper;
import my.hive.domain.notification.model.dto.AnnouncementPublishRequest;
import my.hive.domain.notification.model.entity.EnterpriseAnnouncement;
import my.hive.infrastructure.storage.LocalFileStorageService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.security.InternalStorageReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnterpriseAnnouncementAttachmentTest {
    private final EnterpriseAnnouncementMapper mapper = mock(EnterpriseAnnouncementMapper.class);
    private final EnterpriseAnnouncementService service = new EnterpriseAnnouncementService();

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 1L, Set.of());
        ReflectionTestUtils.setField(service, "enterpriseAnnouncementMapper", mapper);
        ReflectionTestUtils.setField(service, "contextPath", "/api");
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "", "/hive"})
    void publishesActualLocalUploadAndKeepsItDownloadable(String context, @TempDir Path root) throws Exception {
        ReflectionTestUtils.setField(service, "contextPath", context);
        LocalFileStorageService storage = new LocalFileStorageService();
        ReflectionTestUtils.setField(storage, "contextPath", context);
        ReflectionTestUtils.setField(storage, "uploadRoot", root.toString());
        ReflectionTestUtils.setField(storage, "maxFileSizeMb", 1L);
        byte[] content = "announcement attachment".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var upload = storage.upload(new MockMultipartFile("file", "notice.txt", "text/plain", content),
                "TENANT_001", "announcement");

        var result = service.publishAnnouncement(request(upload.getUrl()));

        assertEquals(upload.getUrl(), result.getAttachmentUrl());
        ArgumentCaptor<EnterpriseAnnouncement> saved = ArgumentCaptor.forClass(EnterpriseAnnouncement.class);
        verify(mapper).insertAnnouncement(saved.capture());
        assertEquals(result.getAttachmentUrl(), saved.getValue().getAttachmentUrl());
        try (var input = storage.load(result.getAttachmentUrl(), "TENANT_001", "announcement").getInputStream()) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void acceptsLegacyLocalUrlAndOptionalAttachment() {
        assertEquals("/api/uploads/announcement/TENANT_001/notice.txt",
                service.publishAnnouncement(request("/uploads/announcement/TENANT_001/notice.txt")).getAttachmentUrl());
        assertNull(service.publishAnnouncement(request(null)).getAttachmentUrl());
    }

    @Test
    void acceptsPrivateOssUploadReference() {
        String reference = InternalStorageReference.privateOssReference("/api", "hive/TENANT_001/announcement/notice.txt");
        assertEquals(reference, service.publishAnnouncement(request(reference)).getAttachmentUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/uploads/announcement/TENANT_002/notice.txt",
            "/api/uploads/sales-order/TENANT_001/notice.txt",
            "https://evil.example/uploads/announcement/TENANT_001/notice.txt",
            "/api/uploads/announcement/TENANT_001/../notice.txt",
            "/other/uploads/announcement/TENANT_001/notice.txt"
    })
    void rejectsUnauthorizedReferencesBeforePersisting(String reference) {
        assertThrows(BusinessException.class, () -> service.publishAnnouncement(request(reference)));
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsCrossTenantAndWrongModuleOssReferences() {
        for (String key : Set.of("hive/TENANT_002/announcement/notice.txt", "hive/TENANT_001/sales-order/notice.txt")) {
            assertThrows(BusinessException.class, () -> service.publishAnnouncement(
                    request(InternalStorageReference.privateOssReference("/api", key))));
        }
        verifyNoInteractions(mapper);
    }

    private AnnouncementPublishRequest request(String url) {
        AnnouncementPublishRequest request = new AnnouncementPublishRequest();
        request.setTitle("公告");
        request.setContent("公告内容");
        request.setAttachmentUrl(url);
        request.setAttachmentName("notice.txt");
        return request;
    }
}
