package my.hive.domain.label;

import my.hive.domain.label.model.dto.LabelTemplateSaveRequest;
import my.hive.domain.label.service.LabelTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedLabelServiceTest {

    @Test
    void labelDomainUsesCanonicalSourceRoot() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/label/service/LabelTemplateService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/label/LabelTemplateController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/label")));
        assertFalse(Files.exists(source.resolve("my/management/controller/LabelTemplateController.java")));
    }

    @Test
    void labelServiceKeepsDefaultTemplateContracts() throws Exception {
        assertMethod("variables", String.class);
        assertMethod("list", String.class);
        assertMethod("detail", Long.class);
        assertMethod("defaultTemplate", String.class);
        assertMethod("ensureDefaultsForTenant", String.class, Long.class);
        assertMethod("save", LabelTemplateSaveRequest.class);
        assertMethod("upload", MultipartFile.class, String.class, String.class, Integer.class);
        assertMethod("setDefault", Long.class);
        assertMethod("disable", Long.class);
    }

    private void assertMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = LabelTemplateService.class.getMethod(name, parameterTypes);
        assertNotNull(method);
    }
}
