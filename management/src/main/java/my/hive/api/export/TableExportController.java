package my.hive.api.export;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.utils.ExcelUtil;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exports the current visible frontend table through EasyExcel so downloaded files are real .xlsx files.
 */
@RestController
@RequestMapping("/export")
public class TableExportController {

    private static final int MAX_EXPORT_ROWS = 2000;
    private static final int MAX_EXPORT_COLUMNS = 80;
    private static final int MAX_CELL_LENGTH = 1000;
    private static final Map<String, String> MODULE_PERMISSION_MAP = Map.ofEntries(
            Map.entry("order", PermissionCatalogV3.CODE_ORDER_LIST),
            Map.entry("inventory", PermissionCatalogV3.CODE_INVENTORY_EXPORT),
            Map.entry("customer", PermissionCatalogV3.CODE_CUSTOMER_EXPORT),
            Map.entry("approval-order", PermissionCatalogV3.CODE_ORDER_LIST),
            Map.entry("approval-finance", PermissionCatalogV3.CODE_APPROVAL_FINANCE_LIST),
            Map.entry("approval-leave", PermissionCatalogV3.CODE_APPROVAL_LEAVE_LIST),
            Map.entry("approval-resignation", PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_LIST),
            Map.entry("badproduct", PermissionCatalogV3.CODE_QUALITY_EXPORT),
            Map.entry("document", PermissionCatalogV3.CODE_DOCUMENT_EXPORT),
            Map.entry("role", PermissionCatalogV3.CODE_ROLE_LIST),
            Map.entry("equipment", PermissionCatalogV3.CODE_EQUIPMENT_EXPORT)
    );

    @Resource
    private ExcelUtil excelUtil;

    @PostMapping("/table")
    @CollectLog(module = "table_export", action = "export_current_page", bizType = "table_export", description = "管理端导出当前列表数据", recordArgs = false, recordResult = false)
    public void exportTable(@RequestBody TableExportRequest request, HttpServletResponse response) {
        if (request == null) {
            throw new BusinessException("导出数据不能为空");
        }
        assertModuleExportAllowed(request.sourceModule());
        List<String> headers = normalizeHeaders(request.headers());
        List<List<String>> rows = normalizeRows(request.rows(), headers.size());
        excelUtil.writeRowsToResponse(
                response,
                normalizeName(request.sheetName(), "列表数据"),
                headers,
                rows,
                normalizeFileName(request.fileName())
        );
    }

    private void assertModuleExportAllowed(String sourceModule) {
        String module = sourceModule == null ? "" : sourceModule.trim().toLowerCase();
        String requiredPermission = MODULE_PERMISSION_MAP.get(module);
        if (requiredPermission == null) {
            throw new BusinessException(403, "导出来源不合法，请刷新页面后重试");
        }
        if (!TenantPermissionContext.hasPermission(requiredPermission)) {
            throw new BusinessException(403, "您没有权限导出该页面数据");
        }
    }

    private List<String> normalizeHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new BusinessException("导出表头不能为空");
        }
        if (headers.size() > MAX_EXPORT_COLUMNS) {
            throw new BusinessException("导出列数不能超过 " + MAX_EXPORT_COLUMNS + " 列");
        }
        List<String> normalized = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            String header = normalizeCell(headers.get(i));
            normalized.add(header.isBlank() ? "列" + (i + 1) : header);
        }
        return normalized;
    }

    private List<List<String>> normalizeRows(List<List<String>> rows, int columnCount) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("当前列表暂无可导出的数据");
        }
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw new BusinessException("当前页可导出数据不能超过 " + MAX_EXPORT_ROWS + " 行");
        }
        List<List<String>> normalizedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            List<String> normalizedRow = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                normalizedRow.add(row != null && i < row.size() ? normalizeCell(row.get(i)) : "");
            }
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private String normalizeCell(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return normalized.length() > MAX_CELL_LENGTH ? normalized.substring(0, MAX_CELL_LENGTH) : normalized;
    }

    private String normalizeName(String value, String fallback) {
        String normalized = normalizeCell(value);
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|\\[\\]]", "_");
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeFileName(String fileName) {
        String normalized = normalizeName(fileName, "列表数据");
        if (normalized.toLowerCase().endsWith(".xlsx")) {
            return normalized;
        }
        normalized = normalized.replaceAll("\\.(xls|csv)$", "");
        return normalized + ".xlsx";
    }

    public record TableExportRequest(
            String sheetName,
            String fileName,
            String sourceModule,
            List<String> headers,
            List<List<String>> rows
    ) {
    }
}
