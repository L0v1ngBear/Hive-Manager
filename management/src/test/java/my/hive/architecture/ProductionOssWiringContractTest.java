package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionOssWiringContractTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    @Test
    void businessAndDocumentUploadsUseTheConfiguredStorageRouter() throws IOException {
        String attachments = read("my/hive/infrastructure/storage/BusinessAttachmentService.java");
        String documents = read("my/hive/domain/document/service/DocumentService.java");
        String orders = read("my/hive/domain/order/service/OrderService.java");

        assertTrue(attachments.contains("FileStorageProviderRouter"));
        assertTrue(attachments.contains("storageRouter.upload("));
        assertTrue(attachments.contains("storageRouter.load("));

        assertTrue(documents.contains("FileStorageProviderRouter"));
        assertTrue(documents.contains("storageRouter.upload("));
        assertTrue(documents.contains("storageRouter.load("));
        assertFalse(documents.contains("LocalFileStorageService"));

        assertTrue(orders.contains("businessAttachmentService.upload(file, \"sales-order\")"));
        assertTrue(orders.contains("businessAttachmentService.load(attachmentUrl, \"sales-order\")"));
        assertFalse(orders.contains("file.transferTo(targetPath)"));
    }

    @Test
    void documentDownloadIsAuthenticatedWhileOnlyTenantLogosArePublic() throws IOException {
        String controller = read("my/hive/api/document/DocumentController.java");
        String webMvc = read("my/hive/shared/config/WebMvcConfig.java");
        String publicStorage = read("my/hive/api/storage/PublicStorageController.java");

        assertTrue(controller.contains("@GetMapping(\"/file/download\")"));
        assertTrue(controller.contains("PermissionCatalogV3.CODE_DOCUMENT_FILE_DOWNLOAD"));
        assertTrue(webMvc.contains("\"/storage/public/tenant-logo/**\""));
        assertFalse(webMvc.contains("\"/storage/private/**\""));
        assertTrue(publicStorage.contains("@GetMapping(\"/tenant-logo/{reference}\")"));
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(MAIN.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
