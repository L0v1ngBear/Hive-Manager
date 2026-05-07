package my.management.module.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.annotation.CollectLog;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.common.utils.ExcelUtil;
import my.management.common.vo.ImportResultVO;
import my.management.module.inventory.mapper.ClothMapper;
import my.management.module.inventory.mapper.ClothModelSpecMapper;
import my.management.module.inventory.mapper.InventoryRecordMapper;
import my.management.module.inventory.model.dto.InventoryInRequest;
import my.management.module.inventory.model.dto.InventoryOutRequest;
import my.management.module.inventory.model.dto.InventoryPageRequest;
import my.management.module.inventory.model.entity.Cloth;
import my.management.module.inventory.model.entity.ClothModelSpec;
import my.management.module.inventory.model.entity.InventoryRecord;
import my.management.module.inventory.model.enums.ClothInventoryStatusEnum;
import my.management.module.inventory.model.enums.ClothQualityFlagEnum;
import my.management.module.inventory.model.enums.InventoryInTypeEnum;
import my.management.module.inventory.model.enums.InventoryOperateTypeEnum;
import my.management.module.inventory.model.vo.ClothInventoryVO;
import my.management.module.inventory.model.vo.InventoryModelOptionVO;
import my.management.module.inventory.model.vo.InventoryRecordVO;
import my.management.module.inventory.model.vo.InventorySummaryVO;
import my.management.module.inventory.model.vo.InventoryTrendVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 网页端库存服务，负责布匹入库、出库扣减、库存查询和运营看板数据。
 */
@Service
public class InventoryService {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("100");
    private static final DateTimeFormatter BARCODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final int MAX_IMPORT_ROWS = 5000;
    private static final long MAX_IMPORT_BYTES = 10L * 1024 * 1024;
    private static final BigDecimal MAX_IMPORT_METERS = new BigDecimal("999999999.99");
    private static final int INITIAL_VERSION = 0;
    private static final List<String> IMPORT_TEMPLATE_HEADERS = List.of("条码", "型号", "规格", "总米数", "剩余米数", "入库时间", "状态");
    private static final Map<String, Set<String>> IMPORT_HEADER_ALIASES = buildImportHeaderAliases();
    private static final List<DateTimeFormatter> IMPORT_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Resource
    private ClothMapper clothMapper;

    @Resource
    private InventoryRecordMapper inventoryRecordMapper;

    @Resource
    private ClothModelSpecMapper clothModelSpecMapper;

    @Resource
    private RedisCacheHelper redisCacheHelper;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private ExcelUtil excelUtil;

    private static Map<String, Set<String>> buildImportHeaderAliases() {
        Map<String, Set<String>> aliases = new LinkedHashMap<>();
        aliases.put("barcode", aliasSet("条码", "库存条码", "布匹条码", "卷号", "匹号", "码单号", "编号", "库存编号", "批号", "缸号", "barcode", "bar code"));
        aliases.put("modelCode", aliasSet("型号", "布匹型号", "面料型号", "货号", "款号", "品名", "产品型号", "物料编码", "物料名称", "产品编码", "商品编码", "货品编码", "sku", "model", "modelcode"));
        aliases.put("spec", aliasSet("规格", "克重", "门幅", "幅宽", "宽幅", "规格米", "规格型号", "克重门幅", "spec", "weight", "width"));
        aliases.put("totalMeters", aliasSet("总米数", "入库米数", "米数", "数量", "库存数量", "库存数", "库存", "库存米数", "现有库存", "当前库存", "总数量", "结余", "结存", "库存余额", "meters", "qty", "quantity"));
        aliases.put("remainingMeters", aliasSet("剩余米数", "可用米数", "现存米数", "现存数量", "当前库存", "库存米数", "库存数", "库存", "剩余数量", "可用数量", "可用库存", "可用库存米数", "remaining", "remain"));
        aliases.put("inTime", aliasSet("入库时间", "入仓时间", "入库日期", "日期", "创建时间", "登记时间", "时间", "业务日期", "单据日期", "intime", "date"));
        aliases.put("status", aliasSet("状态", "库存状态", "是否出库", "status"));
        return aliases;
    }

    private static Set<String> aliasSet(String... values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            normalized.add(normalizeHeader(value));
        }
        return normalized;
    }

    public InventorySummaryVO summary() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        InventorySummaryVO summary = clothMapper.selectSummary(tenantCode);
        if (summary == null) {
            summary = new InventorySummaryVO();
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();
        summary.setWarningCount(nvl(clothMapper.countWarningModels(tenantCode, WARNING_THRESHOLD)));
        summary.setTodayInMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, InventoryOperateTypeEnum.IN.getCode(), startTime, endTime)));
        summary.setTodayOutMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, InventoryOperateTypeEnum.OUT.getCode(), startTime, endTime)));
        summary.setTotalMeters(nvl(summary.getTotalMeters()));
        summary.setClothCount(nvl(summary.getClothCount()));
        return summary;
    }

    public PageResult<ClothInventoryVO> page(InventoryPageRequest request) {
        LambdaQueryWrapper<Cloth> wrapper = new LambdaQueryWrapper<>();
        if (request.getStatus() != null) {
            wrapper.eq(Cloth::getStatus, request.getStatus());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim();
            wrapper.and(item -> item.like(Cloth::getBarcode, keyword).or().like(Cloth::getModelCode, keyword));
        }
        wrapper.orderByDesc(Cloth::getUpdateTime).orderByDesc(Cloth::getId);

        Page<Cloth> page = clothMapper.selectPage(
                new Page<>(safePageNum(request.getPageNum()), safePageSize(request.getPageSize())),
                wrapper
        );
        List<ClothInventoryVO> records = page.getRecords().stream().map(this::toClothVO).toList();

        PageResult<ClothInventoryVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(records);
        return result;
    }

    private long safePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public List<InventoryWarningVO> warnings() {
        return clothMapper.selectWarnings(TenantPermissionContext.getTenantCode(), WARNING_THRESHOLD, 8);
    }

    public List<InventoryRecordVO> recentRecords() {
        return inventoryRecordMapper.selectRecent(TenantPermissionContext.getTenantCode(), 12)
                .stream()
                .peek(item -> item.setOperateTypeName(recordOperateTypeName(item.getOperateType())))
                .toList();
    }

    private String recordOperateTypeName(Integer operateType) {
        return InventoryOperateTypeEnum.of(operateType).getLabel();
    }

    public List<InventoryTrendVO> trend() {
        LocalDate endDate = LocalDate.now().plusDays(1);
        LocalDate startDate = endDate.minusDays(7);
        return clothMapper.selectTrend(TenantPermissionContext.getTenantCode(), startDate.atStartOfDay(), endDate.atStartOfDay());
    }

    public List<InventoryModelOptionVO> searchModels(String keyword) {
        return clothModelSpecMapper.search(TenantPermissionContext.getTenantCode(), keyword, 20);
    }

    public ClothInventoryVO searchByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException("请输入条码");
        }
        Cloth cloth = clothMapper.selectOne(new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Cloth::getBarcode, barcode.trim()));
        if (cloth == null) {
            throw new BusinessException("未找到该条码库存");
        }
        return toClothVO(cloth);
    }

    @Transactional(rollbackFor = Exception.class)
    @CollectLog(module = "inventory", action = "cloth_in", bizType = "cloth", bizNo = "#p0.barcode", description = "网页端布匹入库")
    public void in(InventoryInRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        LocalDateTime now = LocalDateTime.now();

        String barcode = request.getBarcode() == null || request.getBarcode().isBlank()
                ? generateBarcode(tenantCode)
                : request.getBarcode().trim();
        if (existsBarcode(tenantCode, barcode)) {
            throw new BusinessException("条码已存在，请更换条码或清空后自动生成");
        }

        Cloth cloth = new Cloth();
        cloth.setTenantCode(tenantCode);
        cloth.setCreateTime(now);
        cloth.setUpdateTime(now);
        cloth.setBarcode(barcode);
        cloth.setModelCode(request.getModelCode().trim());
        cloth.setSpec(request.getSpec());
        cloth.setMeters(request.getMeters());
        cloth.setTotalMeters(request.getMeters());
        cloth.setRemainingMeters(request.getMeters());
        cloth.setStatus(ClothInventoryStatusEnum.IN_STOCK.getCode());
        cloth.setInTime(now);
        cloth.setInOperatorId(userId);
        cloth.setInType(InventoryInTypeEnum.normalizeManual(request.getInType()));
        cloth.setIsBad(ClothQualityFlagEnum.NORMAL.getCode());
        cloth.setVersion(INITIAL_VERSION);
        clothMapper.insert(cloth);

        saveRecord(cloth, InventoryOperateTypeEnum.IN.getCode(), request.getMeters(), userId, now);
        saveModelSpecIfAbsent(tenantCode, cloth.getModelCode(), cloth.getSpec());
        invalidateDashboardCache(tenantCode);
    }

    @Transactional(rollbackFor = Exception.class)
    @CollectLog(module = "inventory", action = "cloth_out", bizType = "cloth", bizNo = "#p0.barcode", description = "网页端布匹出库")
    public void out(InventoryOutRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        LocalDateTime now = LocalDateTime.now();

        Cloth cloth = clothMapper.selectOne(new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, tenantCode)
                .eq(Cloth::getBarcode, request.getBarcode().trim()));
        if (cloth == null) {
            throw new BusinessException("未找到该条码库存");
        }
        if (cloth.getRemainingMeters() == null || cloth.getRemainingMeters().compareTo(request.getMeters()) < 0) {
            throw new BusinessException("库存米数不足，无法出库");
        }

        BigDecimal remaining = cloth.getRemainingMeters().subtract(request.getMeters());
        cloth.setRemainingMeters(remaining);
        cloth.setStatus(ClothInventoryStatusEnum.fromStock(cloth.getTotalMeters(), remaining).getCode());
        cloth.setOutTime(now);
        cloth.setOutOperatorId(userId);
        cloth.setUpdateTime(now);
        clothMapper.updateById(cloth);

        saveRecord(cloth, InventoryOperateTypeEnum.OUT.getCode(), request.getMeters(), userId, now);
        invalidateDashboardCache(tenantCode);
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        List<List<String>> examples = List.of(
                List.of("CL202605060001", "A-2301", "1.50", "120.00", "120.00", LocalDate.now().toString(), "在库"),
                List.of("", "B-8806", "0.00", "86.50", "", LocalDate.now().toString(), "")
        );
        List<String> notes = List.of(
                "这是外部系统库存快照导入，不要求客户按 Hive 模板重新整理。",
                "支持 .xlsx、.xls、.csv 文件导入，单次最多 " + MAX_IMPORT_ROWS + " 行；Excel 会自动扫描多个工作表。",
                "必填：型号，以及总米数/剩余米数/库存米数/数量中的任意一列。",
                "条码可为空，系统会自动生成；如果条码已存在，会按导入文件更新该条库存快照。",
                "剩余米数为空时默认等于总米数；状态为空时系统按剩余米数自动判断。",
                "可适配客户旧系统导出列名：货号、面料型号、物料编码、库存数量、现存米数、可用数量、卷号、匹号、批号、缸号等。",
                "导入是现有库存快照，不要求和系统模板完全一致；不能识别的列会自动忽略。"
        );
        excelUtil.writeTemplateToResponse(response,
                "外部库存导入字段说明",
                IMPORT_TEMPLATE_HEADERS,
                examples,
                notes,
                "外部库存导入字段说明.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    @CollectLog(module = "inventory", action = "inventory_import", bizType = "cloth", description = "网页端库存快照导入")
    public ImportResultVO importInventory(MultipartFile file) {
        validateImportFile(file);
        List<List<String>> rows = readImportRows(file);
        int headerRowIndex = findHeaderRowIndex(rows);
        if (headerRowIndex < 0) {
            throw new BusinessException("未识别到库存表头，请至少包含型号和库存米数/数量相关列");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(headerRowIndex));
        if (!headerIndex.containsKey("modelCode")) {
            throw new BusinessException("导入文件缺少型号列，可使用：型号、货号、面料型号、物料编码");
        }
        if (!headerIndex.containsKey("totalMeters") && !headerIndex.containsKey("remainingMeters")) {
            throw new BusinessException("导入文件缺少米数列，可使用：总米数、库存米数、现存米数、数量、剩余米数");
        }

        ImportResultVO result = new ImportResultVO();
        Set<String> fileBarcodes = new HashSet<>();
        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            if (isRecognizableHeaderRow(row)) {
                continue;
            }
            if (result.getTotalCount() >= MAX_IMPORT_ROWS) {
                throw new BusinessException("单次最多导入 " + MAX_IMPORT_ROWS + " 行库存数据，请拆分文件后重试");
            }
            result.setTotalCount(result.getTotalCount() + 1);
            try {
                InventoryImportRow importRow = parseImportRow(row, headerIndex);
                if (importRow.barcode() != null && !fileBarcodes.add(importRow.barcode())) {
                    throw new BusinessException("条码在导入文件中重复：" + importRow.barcode());
                }
                upsertInventorySnapshot(importRow);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                if (result.getFailMessages().size() < 30) {
                    result.getFailMessages().add("第 " + (i + 1) + " 行：" + ex.getMessage());
                }
            }
        }

        if (result.getTotalCount() == 0) {
            throw new BusinessException("导入文件没有可导入的数据行");
        }
        invalidateDashboardCache(TenantPermissionContext.getTenantCode());
        return result;
    }

    private void upsertInventorySnapshot(InventoryImportRow row) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        String barcode = row.barcode() == null ? generateBarcode(tenantCode) : row.barcode();

        Cloth cloth = clothMapper.selectOne(new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, tenantCode)
                .eq(Cloth::getBarcode, barcode)
                .last("LIMIT 1"));
        boolean created = cloth == null;
        if (created) {
            cloth = new Cloth();
            cloth.setTenantCode(tenantCode);
            cloth.setBarcode(barcode);
            cloth.setCreateTime(now);
            cloth.setInOperatorId(userId);
            cloth.setInType(InventoryInTypeEnum.IMPORT_SNAPSHOT.getCode());
            cloth.setIsBad(ClothQualityFlagEnum.NORMAL.getCode());
            cloth.setVersion(INITIAL_VERSION);
        }

        cloth.setUpdateTime(now);
        cloth.setModelCode(row.modelCode());
        cloth.setSpec(row.spec());
        cloth.setMeters(row.totalMeters());
        cloth.setTotalMeters(row.totalMeters());
        cloth.setRemainingMeters(row.remainingMeters());
        cloth.setStatus(row.status());
        cloth.setInTime(row.inTime() == null ? now : row.inTime());
        if (ClothInventoryStatusEnum.of(row.status()).isOutStock()) {
            cloth.setOutTime(now);
            cloth.setOutOperatorId(userId);
        }

        if (created) {
            clothMapper.insert(cloth);
        } else {
            clothMapper.updateById(cloth);
        }
        saveRecord(cloth, InventoryOperateTypeEnum.EXTERNAL_IMPORT.getCode(), row.remainingMeters(), userId, now);
        saveModelSpecIfAbsent(tenantCode, row.modelCode(), row.spec());
    }

    private ClothInventoryVO toClothVO(Cloth cloth) {
        ClothInventoryVO vo = new ClothInventoryVO();
        BeanUtils.copyProperties(cloth, vo);
        vo.setStatusName(statusName(cloth.getStatus()));
        return vo;
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的库存文件");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new BusinessException("库存导入文件不能超过 10MB");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".csv")) {
            throw new BusinessException("仅支持 .xlsx、.xls、.csv 库存文件");
        }
    }

    private List<List<String>> readImportRows(MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".csv")) {
                return readCsvRows(file);
            }
            return readExcelRows(file);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("库存导入文件读取失败，请检查文件是否损坏或格式是否正确");
        }
    }

    private List<List<String>> readExcelRows(MultipartFile file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (var inputStream = file.getInputStream(); var workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException("库存导入文件没有可读取的工作表");
            }
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null) {
                    continue;
                }
                for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    int lastCellNum = row == null ? 0 : Math.max(row.getLastCellNum(), 0);
                    List<String> values = new ArrayList<>(lastCellNum);
                    for (int j = 0; j < lastCellNum; j++) {
                        values.add(readCellValue(row.getCell(j)));
                    }
                    rows.add(values);
                }
            }
        }
        return rows;
    }

    private List<List<String>> readCsvRows(MultipartFile file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.isEmpty() && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                lines.add(line);
            }
        }
        char delimiter = detectDelimiter(lines);
        return lines.stream().map(line -> parseDelimitedLine(line, delimiter)).toList();
    }

    private String readCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return excelUtil.readString(cell);
    }

    private char detectDelimiter(List<String> lines) {
        String sample = lines.stream().filter(line -> line != null && !line.isBlank()).findFirst().orElse("");
        int tabCount = countChar(sample, '\t');
        int commaCount = countChar(sample, ',');
        int semicolonCount = countChar(sample, ';');
        if (tabCount >= commaCount && tabCount >= semicolonCount && tabCount > 0) {
            return '\t';
        }
        if (semicolonCount > commaCount) {
            return ';';
        }
        return ',';
    }

    private List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private int findHeaderRowIndex(List<List<String>> rows) {
        int limit = Math.min(rows.size(), 100);
        for (int i = 0; i < limit; i++) {
            Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(i));
            boolean hasModel = headerIndex.containsKey("modelCode");
            boolean hasMeters = headerIndex.containsKey("totalMeters") || headerIndex.containsKey("remainingMeters");
            if (hasModel && hasMeters) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRecognizableHeaderRow(List<String> row) {
        Map<String, Integer> headerIndex = buildHeaderIndex(row);
        boolean hasModel = headerIndex.containsKey("modelCode");
        boolean hasMeters = headerIndex.containsKey("totalMeters") || headerIndex.containsKey("remainingMeters");
        return hasModel && hasMeters;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headerRow) {
        Map<String, Integer> index = new HashMap<>();
        if (headerRow == null) {
            return index;
        }
        for (int i = 0; i < headerRow.size(); i++) {
            String column = resolveImportColumn(headerRow.get(i));
            if (column != null) {
                index.putIfAbsent(column, i);
            }
        }
        return index;
    }

    private String resolveImportColumn(String header) {
        String normalized = normalizeHeader(header);
        if (normalized.isBlank()) {
            return null;
        }
        for (Map.Entry<String, Set<String>> entry : IMPORT_HEADER_ALIASES.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private InventoryImportRow parseImportRow(List<String> row, Map<String, Integer> headerIndex) {
        String barcode = cleanText(getCell(row, headerIndex.get("barcode")));
        String modelCode = cleanText(getCell(row, headerIndex.get("modelCode")));
        if (modelCode == null) {
            throw new BusinessException("型号不能为空");
        }
        if (modelCode.length() > 100) {
            throw new BusinessException("型号长度不能超过 100 个字符");
        }
        if (barcode != null && barcode.length() > 100) {
            throw new BusinessException("条码长度不能超过 100 个字符");
        }

        BigDecimal spec = parseNonNegativeDecimal(getCell(row, headerIndex.get("spec")), "规格", true);
        BigDecimal totalMeters = parseNonNegativeDecimal(getCell(row, headerIndex.get("totalMeters")), "总米数", true);
        BigDecimal remainingMeters = parseNonNegativeDecimal(getCell(row, headerIndex.get("remainingMeters")), "剩余米数", true);
        if (totalMeters == null && remainingMeters == null) {
            throw new BusinessException("总米数或剩余米数至少填写一个");
        }
        if (totalMeters == null) {
            totalMeters = remainingMeters;
        }
        if (remainingMeters == null) {
            remainingMeters = totalMeters;
        }
        if (totalMeters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("总米数必须大于 0");
        }
        if (remainingMeters.compareTo(totalMeters) > 0) {
            throw new BusinessException("剩余米数不能大于总米数");
        }

        LocalDateTime inTime = parseImportTime(getCell(row, headerIndex.get("inTime")));
        Integer status = parseImportStatus(getCell(row, headerIndex.get("status")), totalMeters, remainingMeters);
        return new InventoryImportRow(
                barcode,
                modelCode,
                spec == null ? BigDecimal.ZERO : spec,
                totalMeters,
                remainingMeters,
                inTime,
                status
        );
    }

    private BigDecimal parseNonNegativeDecimal(String value, String fieldName, boolean nullable) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            if (nullable) {
                return null;
            }
            throw new BusinessException(fieldName + "不能为空");
        }
        cleaned = cleaned.replace(",", "").replace("米", "").replace("m", "").replace("M", "");
        try {
            BigDecimal number = new BigDecimal(cleaned);
            if (number.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(fieldName + "不能为负数");
            }
            if (number.compareTo(MAX_IMPORT_METERS) > 0) {
                throw new BusinessException(fieldName + "超出系统允许范围");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new BusinessException(fieldName + "必须是数字");
        }
    }

    private LocalDateTime parseImportTime(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }
        for (DateTimeFormatter formatter : IMPORT_TIME_FORMATTERS) {
            try {
                if (formatter.toString().contains("HourOfDay")) {
                    return LocalDateTime.parse(cleaned, formatter);
                }
                return LocalDate.parse(cleaned, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new BusinessException("入库时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
    }

    private Integer parseImportStatus(String value, BigDecimal totalMeters, BigDecimal remainingMeters) {
        String cleaned = cleanText(value);
        if (cleaned != null) {
            String normalized = cleaned.toLowerCase(Locale.ROOT);
            if ("0".equals(normalized) || normalized.contains("在库") || normalized.contains("正常")) {
                return ClothInventoryStatusEnum.IN_STOCK.getCode();
            }
            if ("1".equals(normalized) || normalized.contains("已出") || normalized.contains("出库完成")) {
                return ClothInventoryStatusEnum.OUT_STOCK.getCode();
            }
            if ("2".equals(normalized) || normalized.contains("部分")) {
                return ClothInventoryStatusEnum.PARTIAL_OUT.getCode();
            }
        }
        return ClothInventoryStatusEnum.fromStock(totalMeters, remainingMeters).getCode();
    }

    private String getCell(List<String> row, Integer index) {
        if (row == null || index == null || index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private boolean isEmptyRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        return row.stream().allMatch(value -> value == null || value.trim().isEmpty());
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned) || "undefined".equalsIgnoreCase(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("（米）", "")
                .replace("(米)", "")
                .replace("米数", "米数")
                .replaceAll("[\\s\\u3000:_：/\\\\()（）\\[\\]【】\\-—.]+", "");
    }

    private int countChar(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private void saveRecord(Cloth cloth, Integer operateType, BigDecimal operateMeters, Long userId, LocalDateTime now) {
        InventoryRecord record = new InventoryRecord();
        record.setTenantCode(cloth.getTenantCode());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setClothId(cloth.getId());
        record.setModelCode(cloth.getModelCode());
        record.setOperateType(operateType);
        record.setOperateMeters(operateMeters);
        record.setRemainingMeters(cloth.getRemainingMeters());
        record.setOperatorId(userId);
        inventoryRecordMapper.insert(record);
    }

    private void saveModelSpecIfAbsent(String tenantCode, String modelCode, BigDecimal spec) {
        Long count = clothModelSpecMapper.selectCount(new LambdaQueryWrapper<ClothModelSpec>()
                .eq(ClothModelSpec::getTenantCode, tenantCode)
                .eq(ClothModelSpec::getModelCode, modelCode)
                .eq(ClothModelSpec::getSpec, spec));
        if (count != null && count > 0) {
            return;
        }
        ClothModelSpec modelSpec = new ClothModelSpec();
        modelSpec.setTenantCode(tenantCode);
        modelSpec.setModelCode(modelCode);
        modelSpec.setSpec(spec);
        clothModelSpecMapper.insert(modelSpec);
    }

    private String generateBarcode(String tenantCode) {
        for (int i = 0; i < 5; i++) {
            String barcode = "CL" + BARCODE_DATE_FORMATTER.format(LocalDateTime.now())
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
            if (!existsBarcode(tenantCode, barcode)) {
                return barcode;
            }
        }
        throw new BusinessException("条码生成失败，请稍后重试");
    }

    private boolean existsBarcode(String tenantCode, String barcode) {
        Long count = clothMapper.selectCount(new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, tenantCode)
                .eq(Cloth::getBarcode, barcode));
        return count != null && count > 0;
    }

    private String statusName(Integer status) {
        return ClothInventoryStatusEnum.of(status).getLabel();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private void invalidateDashboardCache(String tenantCode) {
        deleteCacheByPattern(redisKeyBuilder.cachePattern("management", "dashboard", "overview", tenantCode, "*"));
        deleteCacheByPattern(redisKeyBuilder.cachePattern("management", "dashboard", "ai-advice", tenantCode, "*"));
    }

    private void deleteCacheByPattern(String pattern) {
        redisCacheHelper.deleteByPattern(pattern);
    }

    private record InventoryImportRow(
            String barcode,
            String modelCode,
            BigDecimal spec,
            BigDecimal totalMeters,
            BigDecimal remainingMeters,
            LocalDateTime inTime,
            Integer status
    ) {
    }
}
