package my.management.common.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Component
public class ExcelUtil {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public XSSFWorkbook createWorkbook(String sheetName, List<String> headers, List<List<String>> rows) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        XSSFCellStyle headerStyle = buildHeaderStyle(workbook);
        XSSFCellStyle bodyStyle = buildBodyStyle(workbook);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            List<String> values = rows.get(i);
            for (int j = 0; j < values.size(); j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(values.get(j));
                cell.setCellStyle(bodyStyle);
            }
        }

        autoSize(sheet, headers.size());
        return workbook;
    }

    public XSSFWorkbook createTemplateWorkbook(String sheetName,
                                               List<String> headers,
                                               List<List<String>> exampleRows,
                                               List<String> notes) {
        XSSFWorkbook workbook = createWorkbook(sheetName, headers, exampleRows);
        Sheet noteSheet = workbook.createSheet("填写说明");
        XSSFCellStyle headerStyle = buildHeaderStyle(workbook);
        XSSFCellStyle bodyStyle = buildBodyStyle(workbook);

        Row titleRow = noteSheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("说明");
        titleCell.setCellStyle(headerStyle);

        for (int i = 0; i < notes.size(); i++) {
            Row row = noteSheet.createRow(i + 1);
            Cell cell = row.createCell(0);
            cell.setCellValue(notes.get(i));
            cell.setCellStyle(bodyStyle);
        }

        autoSize(noteSheet, 1);
        return workbook;
    }

    public void writeToResponse(HttpServletResponse response, Workbook workbook, String fileName) {
        try (workbook) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    public String readString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    public LocalDate readLocalDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String value = readString(cell);
        if (value.isEmpty()) {
            return null;
        }
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    public String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate localDate) {
            return localDate.format(DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        return String.valueOf(value);
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle buildBodyStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        return style;
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = Math.min(sheet.getColumnWidth(i) + 1024, 256 * 40);
            sheet.setColumnWidth(i, width);
        }
    }
}
