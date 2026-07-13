package my.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InformationChannelReplacementStaticTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main");

    @Test
    void orderAndInstallationContractsShouldExposeInformationChannelOnly() throws IOException {
        List<String> javaFiles = List.of(
                "java/my/management/module/order/model/dto/SalesOrderSaveRequest.java",
                "java/my/management/module/order/model/dto/SalesOrderUpdateRequest.java",
                "java/my/management/module/order/model/dto/SalesOrderPageRequest.java",
                "java/my/management/module/order/model/dto/ProductionOrderSaveRequest.java",
                "java/my/management/module/order/model/dto/ProductionOrderPageRequest.java",
                "java/my/management/module/order/model/entity/SalesOrder.java",
                "java/my/management/module/order/model/entity/ProductionOrder.java",
                "java/my/management/module/order/model/vo/SalesOrderPageVO.java",
                "java/my/management/module/order/model/vo/SalesOrderDetailVO.java",
                "java/my/management/module/order/model/vo/ProductionOrderPageVO.java",
                "java/my/management/module/order/model/vo/ProductionOrderDetailVO.java",
                "java/my/management/module/installation/model/entity/InstallationTask.java",
                "java/my/management/module/installation/model/vo/InstallationTaskVO.java");

        for (String relativePath : javaFiles) {
            String content = source(relativePath);
            assertTrue(content.contains("informationChannel"), "Missing informationChannel: " + relativePath);
            assertFalse(content.contains("deliveryDate"), "Legacy deliveryDate remains: " + relativePath);
            assertFalse(content.contains("deliveryStart"), "Legacy deliveryStart remains: " + relativePath);
            assertFalse(content.contains("deliveryEnd"), "Legacy deliveryEnd remains: " + relativePath);
        }

        for (String relativePath : List.of(
                "java/my/management/module/order/model/entity/SalesOrder.java",
                "java/my/management/module/order/model/entity/ProductionOrder.java",
                "java/my/management/module/installation/model/entity/InstallationTask.java",
                "resources/sql/installation_task.sql",
                "resources/sql/core_performance_indexes.sql")) {
            String content = source(relativePath);
            assertTrue(content.contains("information_channel"), "Missing information_channel: " + relativePath);
            assertFalse(content.contains("delivery_date"), "Legacy delivery_date remains: " + relativePath);
        }

        assertTrue(source("java/my/management/module/order/model/entity/ProductionOrder.java").contains("private String informationChannel;"),
                "Production information channel must be text");
    }

    @Test
    void salesSynchronizationAndPrintPayloadShouldUseInformationChannelOnly() throws IOException {
        String orderService = source("java/my/management/module/order/service/OrderService.java");
        String installationTaskService = source("java/my/management/module/installation/service/InstallationTaskService.java");

        assertTrue(orderService.contains("productionOrder.setInformationChannel(order.getInformationChannel())"),
                "Sales-to-production synchronization must copy informationChannel");
        assertTrue(installationTaskService.contains("task.setInformationChannel(order.getInformationChannel())"),
                "Sales-to-installation synchronization must copy informationChannel");
        assertTrue(orderService.contains("payload.put(\"informationChannel\""),
                "Print payload must expose informationChannel");
        assertFalse(orderService.contains("payload.put(\"deliveryDate\""),
                "Print payload must not expose deliveryDate");
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(MAIN_SOURCE.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
