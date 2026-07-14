package my.hive.domain.quality;

import my.hive.domain.quality.service.QualityService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedQualityServiceTest {

    @Test
    void canonicalQualitySourcesReplaceLegacyBadProductSources() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/quality/service/QualityService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/quality/QualityController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/badproduct/service/BadProductService.java")));
        assertFalse(Files.exists(source.resolve("my/management/controller/BadProductController.java")));
    }

    @Test
    void qualityRecordAndApprovalFlowUseOneTransactionalService() throws Exception {
        assertRollbackTransaction("save", my.hive.domain.quality.model.dto.BadProductSaveRequest.class);
        assertRollbackTransaction("process", my.hive.domain.quality.model.dto.BadProductProcessRequest.class);
        assertRollbackTransaction("approveProcess", String.class);
        assertRollbackTransaction("rejectProcessApproval", String.class);
    }

    @Test
    void attachmentLifecycleNormalizesNamesAndBlankValues() {
        QualityService service = new QualityService();

        assertEquals("quality-attachment",
                ReflectionTestUtils.invokeMethod(service, "normalizeAttachmentName", null, "/api/uploads/bad-product/TENANT/a.png"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "normalizeAttachmentSize", 100L, null));
    }

    @Test
    void duplicateQualitySubmissionGuardRemainsExplicit() throws Exception {
        Method method = QualityService.class.getMethod("hasPendingQualityApproval", String.class);

        assertEquals(boolean.class, method.getReturnType());
    }

    private void assertRollbackTransaction(String name, Class<?>... parameterTypes) throws Exception {
        Method method = QualityService.class.getMethod(name, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, name + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
