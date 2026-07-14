package my.hive.domain.installation;

import my.hive.domain.installation.service.InstallationTaskService;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
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

class UnifiedInstallationServiceTest {

    @Test
    void canonicalInstallationSourcesReplaceLegacyManagementSources() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/installation/service/InstallationTaskService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/installation/InstallationTaskController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/installation/service/InstallationTaskService.java")));
        assertFalse(Files.exists(source.resolve("my/management/controller/InstallationTaskController.java")));
    }

    @Test
    void completedOrderSyncAndStatusTransitionUseRollbackTransactionBoundary() throws Exception {
        assertRollbackTransaction("createOrSyncFromCompletedOrder", my.hive.domain.order.model.entity.SalesOrder.class);
        assertRollbackTransaction("updateStatus", my.hive.domain.installation.model.dto.InstallationTaskStatusUpdateRequest.class);
    }

    @Test
    void attachmentAuthorizationRejectsCrossTenantStoredUrls() {
        InstallationTaskService service = new InstallationTaskService();
        ReflectionTestUtils.setField(service, "contextPath", "/api");

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "normalizeAttachmentUrl",
                "/api/uploads/installation-task/TENANT_002/20260714/photo.png",
                "TENANT_001"
        ));
    }

    private void assertRollbackTransaction(String name, Class<?>... parameterTypes) throws Exception {
        Method method = InstallationTaskService.class.getMethod(name, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, name + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
