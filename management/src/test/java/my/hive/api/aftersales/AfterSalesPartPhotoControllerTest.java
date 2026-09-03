package my.hive.api.aftersales;

import my.hive.infrastructure.storage.BusinessAttachmentService;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.security.InternalUploadUrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSalesPartPhotoControllerTest {

    private AfterSalesController controller;
    private BusinessAttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        controller = new AfterSalesController();
        attachmentService = mock(BusinessAttachmentService.class);
        ReflectionTestUtils.setField(controller, "businessAttachmentService", attachmentService);
    }

    @Test
    void downloadsHistoricalPartPhotoInlineThroughTenantScopedStorage() throws Exception {
        String storedUrl = "/uploads/after-sales-part/TENANT_001/20260903/photo.jpg";
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        };
        when(attachmentService.load(storedUrl, "after-sales-part")).thenReturn(resource);

        ResponseEntity<Resource> response = controller.downloadPartPhoto(storedUrl);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
        assertThat(response.getBody()).isSameAs(resource);
        verify(attachmentService).load(storedUrl, "after-sales-part");

        Method endpoint = AfterSalesController.class.getMethod("downloadPartPhoto", String.class);
        RequirePermission permission = endpoint.getAnnotation(RequirePermission.class);
        assertThat(permission.value()).containsExactly(PermissionCatalogV3.CODE_AFTER_SALES_PART_LIST);
    }

    @Test
    void partPhotoPathsRemainRestrictedToTheCurrentTenantAndModule() {
        assertThat(InternalUploadUrlValidator.normalizeRelativeUploadPath(
                "/uploads/after-sales-part/TENANT_001/20260903/photo.jpg",
                "",
                "TENANT_001",
                "after-sales-part"
        )).isEqualTo("after-sales-part/TENANT_001/20260903/photo.jpg");

        assertThatThrownBy(() -> InternalUploadUrlValidator.normalizeRelativeUploadPath(
                "/uploads/after-sales-part/OTHER_TENANT/20260903/photo.jpg",
                "",
                "TENANT_001",
                "after-sales-part"
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");

        assertThatThrownBy(() -> InternalUploadUrlValidator.normalizeRelativeUploadPath(
                "/uploads/after-sales-repair/TENANT_001/20260903/photo.jpg",
                "",
                "TENANT_001",
                "after-sales-part"
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }
}
