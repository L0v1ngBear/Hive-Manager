package my.hive.architecture;

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
                "java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java",
                "java/my/hive/domain/order/model/dto/SalesOrderUpdateRequest.java",
                "java/my/hive/domain/order/model/dto/SalesOrderPageRequest.java",
                "java/my/hive/domain/order/model/dto/ProductionOrderSaveRequest.java",
                "java/my/hive/domain/order/model/dto/ProductionOrderPageRequest.java",
                "java/my/hive/domain/order/model/entity/SalesOrder.java",
                "java/my/hive/domain/order/model/entity/ProductionOrder.java",
                "java/my/hive/domain/order/model/vo/SalesOrderPageVO.java",
                "java/my/hive/domain/order/model/vo/SalesOrderDetailVO.java",
                "java/my/hive/domain/order/model/vo/ProductionOrderPageVO.java",
                "java/my/hive/domain/order/model/vo/ProductionOrderDetailVO.java",
                "java/my/hive/domain/installation/model/entity/InstallationTask.java",
                "java/my/hive/domain/installation/model/vo/InstallationTaskVO.java");

        for (String relativePath : javaFiles) {
            String content = source(relativePath);
            assertTrue(content.contains("informationChannel"), "Missing informationChannel: " + relativePath);
            assertFalse(content.contains("deliveryDate"), "Legacy deliveryDate remains: " + relativePath);
            assertFalse(content.contains("deliveryStart"), "Legacy deliveryStart remains: " + relativePath);
            assertFalse(content.contains("deliveryEnd"), "Legacy deliveryEnd remains: " + relativePath);
        }

        for (String relativePath : List.of(
                "java/my/hive/domain/order/model/entity/SalesOrder.java",
                "java/my/hive/domain/order/model/entity/ProductionOrder.java",
                "java/my/hive/domain/installation/model/entity/InstallationTask.java")) {
            String content = source(relativePath);
            assertTrue(content.contains("information_channel"), "Missing information_channel: " + relativePath);
            assertFalse(content.contains("delivery_date"), "Legacy delivery_date remains: " + relativePath);
        }

        String migration = Files.readString(
                Path.of("..", "db-migrations", "migrations",
                        "V20260713_001_order_information_channel_and_cancel_reason.sql"),
                StandardCharsets.UTF_8);
        for (String table : List.of("sales_order", "production_order", "installation_task")) {
            assertTrue(migration.contains("CALL hive_migrate_delivery_date_to_information_channel('" + table + "')"),
                    "Versioned migration must converge information_channel for " + table);
        }
        assertTrue(migration.contains("'DROP COLUMN `delivery_date`'"),
                "Versioned migration must retire delivery_date after convergence");

        assertTrue(source("java/my/hive/domain/order/model/entity/ProductionOrder.java").contains("private String informationChannel;"),
                "Production information channel must be text");
    }

    @Test
    void salesSynchronizationAndPrintPayloadShouldUseInformationChannelOnly() throws IOException {
        String orderService = source("java/my/hive/domain/order/service/OrderService.java");
        String installationTaskService = source("java/my/hive/domain/installation/service/InstallationTaskService.java");

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
