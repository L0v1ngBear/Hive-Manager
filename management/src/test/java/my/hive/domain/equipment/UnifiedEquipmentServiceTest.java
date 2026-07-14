package my.hive.domain.equipment;

import my.hive.domain.equipment.model.dto.EquipmentInspectionSubmitRequest;
import my.hive.domain.equipment.model.dto.EquipmentPageRequest;
import my.hive.domain.equipment.model.dto.EquipmentRecordPageRequest;
import my.hive.domain.equipment.model.dto.EquipmentSaveRequest;
import my.hive.domain.equipment.service.EquipmentService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedEquipmentServiceTest {

    @Test
    void equipmentDomainUsesCanonicalSourceRoot() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/equipment/service/EquipmentService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/equipment/EquipmentController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/equipment")));
        assertFalse(Files.exists(source.resolve("my/management/controller/EquipmentController.java")));
    }

    @Test
    void equipmentServiceKeepsInspectionContracts() throws Exception {
        assertMethod("page", EquipmentPageRequest.class);
        assertMethod("detail", Long.class);
        assertMethod("scanTarget", String.class);
        assertMethod("save", EquipmentSaveRequest.class);
        assertMethod("disable", Long.class);
        assertMethod("recordPage", EquipmentRecordPageRequest.class);
        assertMethod("submitInspection", EquipmentInspectionSubmitRequest.class);
    }

    private void assertMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = EquipmentService.class.getMethod(name, parameterTypes);
        assertNotNull(method);
    }
}
