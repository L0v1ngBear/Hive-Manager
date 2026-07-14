package my.hive.domain.inventory;

import my.hive.domain.inventory.service.InventoryService;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedInventoryServiceTest {

    @Test
    void canonicalInventorySourcesReplaceLegacyManagementSources() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/inventory/service/InventoryService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/inventory/InventoryController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/inventory/service/InventoryService.java")));
        assertFalse(Files.exists(source.resolve("my/management/controller/InventoryController.java")));
    }

    @Test
    void clothInOutAndImportShareRollbackTransactionBoundary() throws Exception {
        assertRollbackTransaction("in", my.hive.domain.inventory.model.dto.InventoryInRequest.class);
        assertRollbackTransaction("out", my.hive.domain.inventory.model.dto.InventoryOutRequest.class);
        assertRollbackTransaction("importInventory", org.springframework.web.multipart.MultipartFile.class);
    }

    @Test
    void imageRecognitionRejectsNonImageUploadsBeforePersistence() {
        InventoryService service = new InventoryService();
        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "cloth.txt",
                "text/plain",
                "not an image".getBytes()
        );

        assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateRecognitionImage", upload));
    }

    private void assertRollbackTransaction(String name, Class<?>... parameterTypes) throws Exception {
        Method method = InventoryService.class.getMethod(name, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, name + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
