package my.hive.domain.customer.service;

import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.ImportResultVO;
import my.hive.shared.utils.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerImportProfileTest {

    private final CustomerMapper customerMapper = mock(CustomerMapper.class);
    private final CustomerContactMapper contactMapper = mock(CustomerContactMapper.class);
    private final CustomerProjectMapper projectMapper = mock(CustomerProjectMapper.class);
    private CustomerService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of());
        service = new CustomerService();
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerContactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "customerProjectMapper", projectMapper);
        ReflectionTestUtils.setField(service, "excelUtil", new ExcelUtil());
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void importsCompactCustomerNameAddressAndOpeningDateWorkbook() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("客户");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("客户名称");
            header.createCell(1).setCellValue("客户地址");
            header.createCell(2).setCellValue("开业时间");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("北京示例酒店");
            row.createCell(1).setCellValue("北京市朝阳区示例路 8 号");
            row.createCell(2).setCellValue("2026-08-15");
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }
        when(customerMapper.selectByTenantCodeAndNameForUpdate(eq("TENANT_001"), eq("北京示例酒店"))).thenReturn(null);

        ImportResultVO result = service.importCustomers(new MockMultipartFile(
                "file", "客户.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getCustomerName()).isEqualTo("北京示例酒店");
        assertThat(customerCaptor.getValue().getCustomerAddress()).isEqualTo("北京市朝阳区示例路 8 号");
        assertThat(customerCaptor.getValue().getOpeningDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(customerCaptor.getValue().getCustomerType()).isEqualTo(1);
    }

    @Test
    void downloadedTemplateContainsCustomerAddressAndOpeningDateColumns() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadImportTemplate(response);

        byte[] content = response.getContentAsByteArray();
        assertThat(content).isNotEmpty();
        assertThat(response.getContentLength()).isEqualTo(content.length);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("客户名称");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("客户地址");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue()).isEqualTo("开业时间");
        }
    }
}
