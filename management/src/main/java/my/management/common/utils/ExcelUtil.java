package my.management.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ExcelUtil {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void writeRowsToResponse(HttpServletResponse response,
                                    String sheetName,
                                    List<String> headers,
                                    List<List<String>> rows,
                                    String fileName) {
        prepareDownloadResponse(response, fileName);
        try {
            EasyExcel.write(response.getOutputStream())
                    .head(toEasyExcelHead(headers))
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet(safeSheetName(sheetName))
                    .doWrite(normalizeRows(rows, headerCount(headers)));
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    public void writeTemplateToResponse(HttpServletResponse response,
                                        String sheetName,
                                        List<String> headers,
                                        List<List<String>> exampleRows,
                                        List<String> notes,
                                        String fileName) {
        prepareDownloadResponse(response, fileName);
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .build()) {
            WriteSheet dataSheet = EasyExcel.writerSheet(0, safeSheetName(sheetName))
                    .head(toEasyExcelHead(headers))
                    .build();
            writer.write(normalizeRows(exampleRows, headerCount(headers)), dataSheet);

            WriteSheet noteSheet = EasyExcel.writerSheet(1, "填写说明")
                    .head(toEasyExcelHead(List.of("说明")))
                    .build();
            writer.write(toSingleColumnRows(notes), noteSheet);
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 模板失败", e);
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

    private void prepareDownloadResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String safeFileName = fileName == null || fileName.isBlank() ? "export.xlsx" : fileName.trim();
        String encoded = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    private List<List<String>> toEasyExcelHead(List<String> headers) {
        List<List<String>> head = new ArrayList<>();
        if (headers == null || headers.isEmpty()) {
            head.add(List.of("数据"));
            return head;
        }
        for (String header : headers) {
            head.add(List.of(stringify(header)));
        }
        return head;
    }

    private List<List<String>> normalizeRows(List<List<String>> rows, int columnCount) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<List<String>> normalizedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            int actualColumnCount = columnCount > 0 ? columnCount : (row == null ? 0 : row.size());
            List<String> normalizedRow = new ArrayList<>(actualColumnCount);
            for (int i = 0; i < actualColumnCount; i++) {
                normalizedRow.add(row != null && i < row.size() ? stringify(row.get(i)) : "");
            }
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private List<List<String>> toSingleColumnRows(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(value -> List.of(stringify(value)))
                .toList();
    }

    private String safeSheetName(String sheetName) {
        String normalized = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName.trim();
        normalized = normalized.replaceAll("[\\\\/?*\\[\\]:]", "_");
        return normalized.length() <= 31 ? normalized : normalized.substring(0, 31);
    }

    private int headerCount(List<String> headers) {
        return headers == null ? 0 : headers.size();
    }
}
