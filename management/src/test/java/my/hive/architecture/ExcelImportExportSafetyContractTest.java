package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelImportExportSafetyContractTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java");

    @Test
    void excelWritersDoNotDependOnServerFontMetricsOrCommitResponsesEarly() throws IOException {
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(MAIN_SOURCE)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                String content = read(path);
                if (content.contains("autoSizeColumn")) {
                    violations.add(path + " uses autoSizeColumn");
                }
                if (content.contains("SheetUtil")) {
                    violations.add(path + " uses POI SheetUtil font metrics");
                }
                if (content.contains("LongestMatchColumnWidthStyleStrategy")) {
                    violations.add(path + " uses dynamic Excel column widths");
                }
                if (content.contains("EasyExcel.write(response.getOutputStream())")) {
                    violations.add(path + " writes a workbook after the HTTP response is exposed");
                }
            });
        }
        assertThat(violations).isEmpty();

        String excelUtil = read(MAIN_SOURCE.resolve("my/hive/shared/utils/ExcelUtil.java"));
        assertThat(excelUtil).contains(
                "SimpleColumnWidthStyleStrategy",
                "writeRowsToBytes",
                "writeTemplateToBytes",
                "response.setContentLength(content.length)"
        );
    }

    @Test
    void everyExcelExportAndTemplateDownloadUsesTheSafeSharedWriter() {
        assertContains("my/hive/api/export/TableExportController.java", "excelUtil.writeRowsToResponse");
        assertContains("my/hive/domain/attendance/service/AttendanceService.java", "excelUtil.writeRowsToResponse");
        assertContains("my/hive/domain/employee/service/EmployeeService.java", "excelUtil.writeRowsToResponse");
        assertContains("my/hive/domain/price/service/PriceService.java", "excelUtil.writeRowsToResponse");
        assertContains("my/hive/domain/aftersales/service/AfterSalesService.java", "excelUtil.writeRowsToBytes");

        assertContains("my/hive/domain/customer/service/CustomerService.java", "excelUtil.writeTemplateToResponse");
        assertContains("my/hive/domain/employee/service/EmployeeService.java", "excelUtil.writeTemplateToResponse");
        assertContains("my/hive/domain/price/service/PriceService.java", "excelUtil.writeTemplateToResponse");
        assertContains("my/hive/domain/inventory/service/InventoryService.java", "excelUtil.writeTemplateToResponse");
    }

    private void assertContains(String relativePath, String expected) {
        assertThat(read(MAIN_SOURCE.resolve(relativePath))).contains(expected);
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Unable to inspect " + path, exception);
        }
    }
}
