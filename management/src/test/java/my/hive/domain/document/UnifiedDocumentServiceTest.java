package my.hive.domain.document;

import my.hive.domain.document.model.dto.DocumentAddRequest;
import my.hive.domain.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedDocumentServiceTest {

    @Test
    void documentDomainUsesCanonicalSourceRoot() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/document/service/DocumentService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/document/DocumentController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/document")));
        assertFalse(Files.exists(source.resolve("my/management/controller/DocumentController.java")));
    }

    @Test
    void documentServiceKeepsMoveRenameAndUploadContracts() throws Exception {
        assertMethod("selectDocumentByParentId", Long.class);
        assertMethod("addFolder", DocumentAddRequest.class);
        assertMethod("uploadFile", MultipartFile.class, Long.class);
        assertMethod("renameDocument", Long.class, String.class);
        assertMethod("moveDocument", Long.class, Long.class);
        assertMethod("getBreadcrumbs", Long.class);
    }

    private void assertMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = DocumentService.class.getMethod(name, parameterTypes);
        assertNotNull(method);
    }
}
