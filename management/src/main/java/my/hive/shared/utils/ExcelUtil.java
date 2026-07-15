package my.hive.shared.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.exception.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.Objects;

@Component
public class ExcelUtil {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXCEL_WRITE_BATCH_SIZE = 500;

    public void writeRowsToResponse(HttpServletResponse response,
                                    String sheetName,
                                    List<String> headers,
                                    List<List<String>> rows,
                                    String fileName) {
        prepareDownloadResponse(response, fileName);
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet(safeSheetName(sheetName))
                    .head(toEasyExcelHead(headers))
                    .build();
            writeRowsInBatches(writer, writeSheet, rows, headerCount(headers));
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

    public void validateXlsxImportFile(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择要导入的 Excel 文件");
        }
        if (maxBytes > 0 && file.getSize() > maxBytes) {
            long maxMb = Math.max(1, maxBytes / 1024 / 1024);
            throw new BusinessException("导入文件不能超过 " + maxMb + "MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.trim().toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式，请先下载系统导入模板");
        }
    }

    public void validateImportHeader(Row headerRow, List<String> expectedHeaders) {
        validateImportHeaderOptions(headerRow, expectedHeaders == null ? List.of() : List.of(expectedHeaders));
    }

    public void validateImportHeaderOptions(Row headerRow, List<List<String>> acceptedHeaders) {
        if (headerRow == null) {
            throw new BusinessException("导入文件缺少表头，请下载最新模板");
        }
        if (acceptedHeaders == null || acceptedHeaders.isEmpty()) {
            throw new BusinessException("导入模板未配置表头");
        }
        for (List<String> expectedHeaders : acceptedHeaders) {
            if (matchesImportHeader(headerRow, expectedHeaders)) {
                return;
            }
        }
        List<String> expectedHeaders = acceptedHeaders.get(acceptedHeaders.size() - 1);
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String actual = normalizeImportHeaderText(readString(headerRow.getCell(i)));
            String expected = normalizeImportHeaderText(expectedHeaders.get(i));
            if (!Objects.equals(expected, actual)) {
                throw new BusinessException("导入表头不匹配，第 " + (i + 1) + " 列应为：" + expectedHeaders.get(i));
            }
        }
    }

    public void validateImportDataRows(Sheet sheet, int cellCount, int maxRows) {
        if (sheet == null) {
            throw new BusinessException("导入文件没有工作表");
        }
        int dataRows = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, cellCount)) {
                continue;
            }
            dataRows++;
            if (maxRows > 0 && dataRows > maxRows) {
                throw new BusinessException("单次最多导入 " + maxRows + " 行，请拆分文件后重试");
            }
        }
        if (dataRows == 0) {
            throw new BusinessException("导入文件没有有效数据行");
        }
    }

    public boolean isEmptyRow(Row row, int cellCount) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < cellCount; i++) {
            if (StringUtils.hasText(readString(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesImportHeader(Row headerRow, List<String> expectedHeaders) {
        if (headerRow == null || expectedHeaders == null || expectedHeaders.isEmpty()) {
            return false;
        }
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String actual = normalizeImportHeaderText(readString(headerRow.getCell(i)));
            String expected = normalizeImportHeaderText(expectedHeaders.get(i));
            if (!Objects.equals(expected, actual)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeImportHeaderText(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
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
            normalizedRows.add(normalizeRow(row, columnCount));
        }
        return normalizedRows;
    }

    private void writeRowsInBatches(ExcelWriter writer, WriteSheet writeSheet, List<List<String>> rows, int columnCount) {
        if (rows == null || rows.isEmpty()) {
            writer.write(List.of(), writeSheet);
            return;
        }
        List<List<String>> batch = new ArrayList<>(Math.min(EXCEL_WRITE_BATCH_SIZE, rows.size()));
        for (List<String> row : rows) {
            batch.add(normalizeRow(row, columnCount));
            if (batch.size() >= EXCEL_WRITE_BATCH_SIZE) {
                writer.write(batch, writeSheet);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            writer.write(batch, writeSheet);
        }
    }

    private List<String> normalizeRow(List<String> row, int columnCount) {
        int actualColumnCount = columnCount > 0 ? columnCount : (row == null ? 0 : row.size());
        List<String> normalizedRow = new ArrayList<>(actualColumnCount);
        for (int i = 0; i < actualColumnCount; i++) {
            normalizedRow.add(row != null && i < row.size() ? stringify(row.get(i)) : "");
        }
        return normalizedRow;
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
