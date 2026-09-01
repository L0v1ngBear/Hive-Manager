package my.hive.domain.importexport;

import jakarta.servlet.http.HttpServletResponse;
import my.hive.domain.customer.service.CustomerService;
import my.hive.domain.employee.service.EmployeeService;
import my.hive.domain.inventory.service.InventoryService;
import my.hive.domain.price.service.PriceService;
import my.hive.domain.tenant.service.TenantFieldConfigService;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.utils.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportTemplateDownloadContractTest {

    private final ExcelUtil excelUtil = new ExcelUtil();
    private final TenantFieldConfigService tenantFieldConfigService = mock(TenantFieldConfigService.class);

    @BeforeEach
    void setUp() {
        when(tenantFieldConfigService.currentFieldLabelMap(anyString())).thenReturn(Map.of());
    }

    @Test
    void customerTemplateIsCompleteAndAcceptedByCustomerImporter() throws Exception {
        CustomerService service = new CustomerService();
        ReflectionTestUtils.setField(service, "excelUtil", excelUtil);

        assertTemplateContract(
                service::downloadImportTemplate,
                file -> service.importCustomers(file),
                List.of("客户名称", "客户地址", "开业时间", "客户类型", "联系人", "联系电话", "项目名称", "施工区域", "项目负责人"),
                "导入文件没有有效数据行"
        );
    }

    @Test
    void employeeTemplateIsCompleteAndAcceptedByEmployeeImporter() throws Exception {
        EmployeeService service = new EmployeeService();
        ReflectionTestUtils.setField(service, "excelUtil", excelUtil);
        ReflectionTestUtils.setField(service, "tenantFieldConfigService", tenantFieldConfigService);

        assertTemplateContract(
                service::downloadImportTemplate,
                file -> service.importEmployees(file),
                List.of("姓名", "手机号", "部门", "职位", "状态", "员工类型", "入职日期", "邮箱", "直属领导姓名", "角色名称", "备注"),
                "导入文件没有有效数据行"
        );
    }

    @Test
    void priceTemplateIsCompleteAndAcceptedByPriceImporter() throws Exception {
        PriceService service = new PriceService();
        ReflectionTestUtils.setField(service, "excelUtil", excelUtil);

        assertTemplateContract(
                service::downloadImportTemplate,
                file -> service.importPrices(file),
                List.of("面料型号", "批号", "规格", "基准价", "币种", "生效日期", "备注"),
                "导入文件没有有效数据行"
        );
    }

    @Test
    void inventoryTemplateIgnoresInstructionSheetAndIsAcceptedByInventoryImporter() throws Exception {
        InventoryService service = new InventoryService();
        ReflectionTestUtils.setField(service, "excelUtil", excelUtil);
        ReflectionTestUtils.setField(service, "tenantFieldConfigService", tenantFieldConfigService);

        assertTemplateContract(
                service::downloadImportTemplate,
                file -> service.importInventory(file),
                List.of("条码", "型号", "规格", "总米数", "剩余米数", "入库时间", "状态"),
                "导入文件没有可导入的数据行"
        );
    }

    private void assertTemplateContract(TemplateDownloader downloader,
                                        TemplateImporter importer,
                                        List<String> expectedHeaders,
                                        String expectedEmptyDataMessage) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        downloader.download(response);

        byte[] content = response.getContentAsByteArray();
        assertThat(content).isNotEmpty();
        assertThat(response.getContentLength()).isEqualTo(content.length);
        assertThat(response.getContentType()).contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).contains("filename*=UTF-8''");

        byte[] headerOnlyTemplate;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("填写说明");
            for (int column = 0; column < expectedHeaders.size(); column++) {
                assertThat(workbook.getSheetAt(0).getRow(0).getCell(column).getStringCellValue())
                        .isEqualTo(expectedHeaders.get(column));
            }
            for (int row = workbook.getSheetAt(0).getLastRowNum(); row >= 1; row--) {
                if (workbook.getSheetAt(0).getRow(row) != null) {
                    workbook.getSheetAt(0).removeRow(workbook.getSheetAt(0).getRow(row));
                }
            }
            workbook.write(output);
            headerOnlyTemplate = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "系统导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                headerOnlyTemplate
        );
        assertThatThrownBy(() -> importer.importFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(expectedEmptyDataMessage);
    }

    @FunctionalInterface
    private interface TemplateDownloader {
        void download(HttpServletResponse response);
    }

    @FunctionalInterface
    private interface TemplateImporter {
        void importFile(MultipartFile file);
    }
}
