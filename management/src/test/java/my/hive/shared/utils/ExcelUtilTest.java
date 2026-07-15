package my.hive.shared.utils;

import my.hive.shared.exception.BusinessException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelUtilTest {

    private final ExcelUtil excelUtil = new ExcelUtil();

    @Test
    void writeTemplateToResponseGeneratesReadableXlsxWithInstructionSheet() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        excelUtil.writeTemplateToResponse(
                response,
                "外部库存导入字段说明",
                List.of("条码", "型号", "规格", "总米数", "剩余米数", "入库时间", "状态"),
                List.of(List.of("CL202605060001", "A-2301", "1.50", "120.00", "120.00", "2026-05-14", "在库")),
                List.of("一行代表一匹布；条码可为空，系统会自动生成。"),
                "外部库存导入字段说明.xlsx"
        );

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0);
        assertTrue(response.getContentType().contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertTrue(response.getHeader("Content-Disposition").contains("filename*=UTF-8''"));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertEquals("外部库存导入字段说明", workbook.getSheetAt(0).getSheetName());
            assertEquals("填写说明", workbook.getSheetAt(1).getSheetName());
            assertEquals("条码", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("型号", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("说明", workbook.getSheetAt(1).getRow(0).getCell(0).getStringCellValue());
            assertEquals("一行代表一匹布；条码可为空，系统会自动生成。", workbook.getSheetAt(1).getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void writeRowsToResponsePadsShortRowsAndKeepsWorkbookReadable() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        excelUtil.writeRowsToResponse(
                response,
                "员工列表",
                List.of("姓名", "手机号", "部门"),
                List.of(List.of("张三", "13900030001")),
                "员工列表.xlsx"
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("员工列表", workbook.getSheetAt(0).getSheetName());
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("张三", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void validateImportHeaderAcceptsMultipleHeaderVariantsAndRejectsMismatch() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Phone");
            header.createCell(2).setCellValue("Department");

            excelUtil.validateImportHeaderOptions(
                    header,
                    List.of(
                            List.of("姓名", "手机号", "部门"),
                            List.of("Name", "Phone", "Department")
                    )
            );

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> excelUtil.validateImportHeader(header, List.of("姓名", "手机号", "部门")));
            assertTrue(exception.getMessage().contains("导入表头不匹配"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void validateImportDataRowsHandlesEmptyRowsAndMaxRows() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("Name");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Alice");
            sheet.createRow(2);

            assertFalse(excelUtil.isEmptyRow(row, 1));
            assertTrue(excelUtil.isEmptyRow(sheet.getRow(2), 1));
            excelUtil.validateImportDataRows(sheet, 1, 1);

            sheet.getRow(2).createCell(0).setCellValue("Bob");
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> excelUtil.validateImportDataRows(sheet, 1, 1));
            assertTrue(exception.getMessage().contains("单次最多导入"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
