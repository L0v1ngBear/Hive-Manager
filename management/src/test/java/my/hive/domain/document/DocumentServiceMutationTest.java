package my.hive.domain.document;

import my.hive.domain.document.mapper.DocumentMapper;
import my.hive.domain.document.model.entity.Document;
import my.hive.domain.document.service.DocumentService;
import my.hive.infrastructure.storage.FileStorageProviderRouter;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceMutationTest {

    private DocumentMapper documentMapper;
    private FileStorageProviderRouter storageRouter;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        storageRouter = mock(FileStorageProviderRouter.class);
        service = new DocumentService();
        ReflectionTestUtils.setField(service, "documentMapper", documentMapper);
        ReflectionTestUtils.setField(service, "storageRouter", storageRouter);
        TenantPermissionContext.init("TENANT_001", 18L, Set.of());
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void deletesOnlyTheTenantFileAndRemovesItsStoredObject() {
        Document document = document(42L, "TENANT_001", DocumentTypeEnum.FILE.getType());
        document.setStorageProvider("ALIYUN_OSS");
        document.setStorageObjectKey("hive/TENANT_001/document/file.pdf");
        when(documentMapper.selectById(42L)).thenReturn(document);

        service.deleteDocument(42L);

        verify(documentMapper).deleteById(42L);
        verify(storageRouter).deleteQuietly("ALIYUN_OSS", "hive/TENANT_001/document/file.pdf");
    }

    @Test
    void rejectsDeletingAnotherTenantsDocument() {
        when(documentMapper.selectById(42L))
                .thenReturn(document(42L, "TENANT_002", DocumentTypeEnum.FILE.getType()));

        assertThatThrownBy(() -> service.deleteDocument(42L))
                .isInstanceOf(BusinessException.class);

        verify(documentMapper, never()).deleteById(any(Long.class));
        verify(storageRouter, never()).deleteQuietly(any(), any());
    }

    @Test
    void rejectsDeletingANonEmptyFolder() {
        when(documentMapper.selectById(42L))
                .thenReturn(document(42L, "TENANT_001", DocumentTypeEnum.FOLDER.getType()));
        when(documentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteDocument(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("清空文件夹");

        verify(documentMapper, never()).deleteById(any(Long.class));
    }

    private Document document(Long id, String tenantCode, Integer type) {
        Document document = new Document();
        document.setId(id);
        document.setTenantCode(tenantCode);
        document.setType(type);
        return document;
    }
}
