package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PrintEndpointUniquenessTest {

    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final List<String> DOMAINS = List.of("customer", "document", "equipment", "label", "print");

    @Test
    void taskSevenDomainsUseCanonicalHiveApiAndDomainPackages() {
        for (String domain : DOMAINS) {
            assertThat(MAIN_SOURCE.resolve("my/hive/api/" + domain)).exists();
            assertThat(MAIN_SOURCE.resolve("my/hive/domain/" + domain)).exists();
        }
    }

    @Test
    void printTaskControllerExistsExactlyOnceInCanonicalApiPackage() throws IOException {
        List<Path> controllers = Files.walk(MAIN_SOURCE)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals("PrintTaskController.java"))
                .toList();

        assertThat(controllers)
                .containsExactly(MAIN_SOURCE.resolve("my/hive/api/print/PrintTaskController.java"));
    }

    @Test
    void taskSevenLegacyRuntimePackagesAreRetired() throws IOException {
        assertThat(MAIN_SOURCE.resolve("my/management/controller/CustomerController.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/DocumentController.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/EquipmentController.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/LabelTemplateController.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/ReceiptPrintController.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/customer")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/document")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/equipment")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/label")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/receipt")).doesNotExist();
    }

    @Test
    void taskSevenRuntimeDoesNotImportLegacyOrMiniPackages() throws IOException {
        Pattern legacy = Pattern.compile("my\\.management\\.module\\.(customer|document|equipment|label|receipt)|my\\.hive_back");
        List<String> offenders = java.util.stream.Stream.concat(Files.walk(MAIN_SOURCE), Files.walk(MAIN_RESOURCES))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java")
                        || path.toString().endsWith(".yaml")
                        || path.toString().endsWith(".yml")
                        || path.toString().endsWith(".properties"))
                .flatMap(path -> {
                    try {
                        return Files.readAllLines(path).stream()
                                .filter(line -> legacy.matcher(line).find())
                                .map(line -> path + ": " + line.trim());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .toList();

        assertThat(offenders).isEmpty();
    }
}
