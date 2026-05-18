package my.management.module.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.common.annotation.CollectLog;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.hive.common.print.PrintTaskService;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.common.storage.BusinessAttachmentService;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.common.utils.ExcelUtil;
import my.management.module.tenant.service.TenantFieldConfigService;
import my.management.module.tenant.model.vo.TenantFieldConfigVO;
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
import my.management.module.inventory.model.vo.InventoryImportResultVO;
import my.management.module.inventory.model.vo.InventoryImageRecognitionVO;
import my.management.module.inventory.model.vo.InventoryInResultVO;
import my.management.module.inventory.model.vo.InventoryLabelTaskVO;
import my.management.module.inventory.model.vo.InventoryModelSummaryVO;
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
import org.springframework.util.StringUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页端库存服务，负责布匹入库、出库扣减、库存查询和运营看板数据。
 */
@Service
public class InventoryService {

    private static final DateTimeFormatter BARCODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final String TIME_ORDER_FIFO = "fifo";
    private static final String TIME_ORDER_LIFO = "lifo";
    private static final int MAX_IMPORT_ROWS = 5000;
    private static final long MAX_IMPORT_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_RECOGNITION_BYTES = 5L * 1024 * 1024;
    private static final BigDecimal MAX_IMPORT_METERS = new BigDecimal("999999999.99");
    private static final int INITIAL_VERSION = 0;
    private static final int MAX_CUSTOM_FIELD_VALUE_LENGTH = 500;
    private static final String IMAGE_RECOGNITION_MODULE = "inventory-recognition";
    private static final Set<String> IMAGE_RECOGNITION_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Pattern METERS_FILENAME_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d{1,2})?)\\s*(?:m|meter|meters|米)");
    private static final Pattern SPEC_FILENAME_PATTERN = Pattern.compile("(?i)(?:spec|规格|克重|门幅|幅宽|width)[-_\\s]*(\\d+(?:\\.\\d{1,2})?)");
    private static final TypeReference<Map<String, Object>> CUSTOM_FIELD_TYPE_REFERENCE = new TypeReference<>() {};
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

    @Resource
    private PrintTaskService printTaskService;

    @Resource
    private TenantFieldConfigService tenantFieldConfigService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private InventorySettingService inventorySettingService;

    @Resource
    private InventoryWarningCacheService inventoryWarningCacheService;

    @Resource
    private BusinessAttachmentService businessAttachmentService;

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

    private List<String> inventoryImportTemplateHeaders(Map<String, String> fieldLabels) {
        List<String> headers = new ArrayList<>(List.of(
                fieldLabel(fieldLabels, "barCode", "条码"),
                fieldLabel(fieldLabels, "modelCode", "型号"),
                fieldLabel(fieldLabels, "spec", "规格"),
                fieldLabel(fieldLabels, "totalMeters", "总米数"),
                fieldLabel(fieldLabels, "remainingMeters", "剩余米数"),
                "入库时间",
                fieldLabel(fieldLabels, "status", "状态")
        ));
        if (fieldLabels != null) {
            fieldLabels.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getKey().startsWith("custom_"))
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .filter(label -> label != null && !label.trim().isBlank())
                    .forEach(headers::add);
        }
        return headers;
    }

    private String fieldLabel(Map<String, String> fieldLabels, String fieldKey, String fallback) {
        if (fieldLabels == null || fieldKey == null || fieldKey.isBlank()) {
            return fallback;
        }
        String label = fieldLabels.get(fieldKey);
        return label == null || label.trim().isBlank() ? fallback : label.trim();
    }

    private Map<String, String> currentTenantFieldLabels(String moduleCode) {
        try {
            return tenantFieldConfigService.currentFieldLabelMap(moduleCode);
        } catch (BusinessException ignored) {
            return Map.of();
        }
    }

    private Map<String, Set<String>> tenantAwareImportHeaderAliases() {
        Map<String, Set<String>> aliases = new LinkedHashMap<>();
        IMPORT_HEADER_ALIASES.forEach((column, values) -> aliases.put(column, new HashSet<>(values)));
        Map<String, String> labels = currentTenantFieldLabels("inventory");
        addTenantAlias(aliases, "barcode", labels.get("barCode"));
        addTenantAlias(aliases, "modelCode", labels.get("modelCode"));
        addTenantAlias(aliases, "spec", labels.get("spec"));
        addTenantAlias(aliases, "totalMeters", labels.get("totalMeters"));
        addTenantAlias(aliases, "remainingMeters", labels.get("remainingMeters"));
        addTenantAlias(aliases, "status", labels.get("status"));
        labels.forEach((fieldKey, label) -> {
            if (fieldKey != null && fieldKey.startsWith("custom_")) {
                addTenantAlias(aliases, fieldKey, label);
                addTenantAlias(aliases, fieldKey, fieldKey);
            }
        });
        return aliases;
    }

    private void addTenantAlias(Map<String, Set<String>> aliases, String column, String label) {
        if (label == null || label.trim().isBlank()) {
            return;
        }
        aliases.computeIfAbsent(column, key -> new HashSet<>()).add(normalizeHeader(label));
    }

    public InventorySummaryVO summary() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        BigDecimal warningThreshold = inventorySettingService.warningThreshold(tenantCode);
        InventorySummaryVO summary = clothMapper.selectSummary(tenantCode);
        if (summary == null) {
            summary = new InventorySummaryVO();
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();
        summary.setWarningThresholdMeters(warningThreshold);
        summary.setWarningCount(inventoryWarningCacheService.countWarningModels(tenantCode));
        summary.setTodayInMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, InventoryOperateTypeEnum.IN.getCode(), startTime, endTime)));
        summary.setTodayOutMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, InventoryOperateTypeEnum.OUT.getCode(), startTime, endTime)));
        summary.setTotalMeters(nvl(summary.getTotalMeters()));
        summary.setClothCount(nvl(summary.getClothCount()));
        return summary;
    }

    public PageResult<ClothInventoryVO> page(InventoryPageRequest request) {
        if (request == null) {
            request = new InventoryPageRequest();
        }
        LambdaQueryWrapper<Cloth> wrapper = new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, TenantPermissionContext.getTenantCode());
        if (request.getStatus() != null) {
            wrapper.eq(Cloth::getStatus, request.getStatus());
        }
        if (request.getSpecMin() != null) {
            wrapper.ge(Cloth::getSpec, request.getSpecMin());
        }
        if (request.getSpecMax() != null) {
            wrapper.le(Cloth::getSpec, request.getSpecMax());
        }
        if (request.getRemainingMin() != null) {
            wrapper.ge(Cloth::getRemainingMeters, request.getRemainingMin());
        }
        if (request.getRemainingMax() != null) {
            wrapper.le(Cloth::getRemainingMeters, request.getRemainingMax());
        }
        if (request.getUpdatedStart() != null) {
            wrapper.ge(Cloth::getUpdateTime, request.getUpdatedStart().atStartOfDay());
        }
        if (request.getUpdatedEnd() != null) {
            wrapper.lt(Cloth::getUpdateTime, request.getUpdatedEnd().plusDays(1).atStartOfDay());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().trim();
            wrapper.and(item -> item.like(Cloth::getBarcode, keyword).or().like(Cloth::getModelCode, keyword));
        }
        applyInventoryTimeOrder(wrapper, request.getTimeOrder());

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

    public PageResult<InventoryModelSummaryVO> modelPage(InventoryPageRequest request) {
        if (request == null) {
            request = new InventoryPageRequest();
        }
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank() ? null : request.getKeyword().trim();
        Integer status = request.getStatus();
        Page<InventoryModelSummaryVO> page = clothMapper.selectModelSummaryPage(
                new Page<>(safePageNum(request.getPageNum()), safePageSize(request.getPageSize())),
                TenantPermissionContext.getTenantCode(),
                keyword,
                status,
                request.getSpecMin(),
                request.getSpecMax(),
                request.getRemainingMin(),
                request.getRemainingMax(),
                request.getUpdatedStart() == null ? null : request.getUpdatedStart().atStartOfDay(),
                request.getUpdatedEnd() == null ? null : request.getUpdatedEnd().plusDays(1).atStartOfDay(),
                normalizeTimeOrder(request.getTimeOrder())
        );
        PageResult<InventoryModelSummaryVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return result;
    }

    public List<ClothInventoryVO> modelDetail(String modelCode, BigDecimal spec, Integer status, String timeOrder) {
        String safeModelCode = cleanText(modelCode);
        if (safeModelCode == null) {
            throw new BusinessException("型号不能为空");
        }
        LambdaQueryWrapper<Cloth> wrapper = new LambdaQueryWrapper<Cloth>()
                .eq(Cloth::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Cloth::getModelCode, safeModelCode)
                .eq(spec != null, Cloth::getSpec, spec)
                .eq(status != null, Cloth::getStatus, status)
                .gt(status == null, Cloth::getRemainingMeters, BigDecimal.ZERO);
        applyInventoryTimeOrder(wrapper, timeOrder);
        return clothMapper.selectList(wrapper).stream().map(this::toClothVO).toList();
    }

    private void applyInventoryTimeOrder(LambdaQueryWrapper<Cloth> wrapper, String timeOrder) {
        String normalized = normalizeTimeOrder(timeOrder);
        if (TIME_ORDER_LIFO.equals(normalized)) {
            wrapper.orderByDesc(Cloth::getInTime).orderByDesc(Cloth::getId);
            return;
        }
        wrapper.orderByAsc(Cloth::getInTime).orderByAsc(Cloth::getId);
    }

    private String normalizeTimeOrder(String timeOrder) {
        String normalized = cleanText(timeOrder);
        if (TIME_ORDER_LIFO.equalsIgnoreCase(normalized)) {
            return TIME_ORDER_LIFO;
        }
        return TIME_ORDER_FIFO;
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
        String tenantCode = TenantPermissionContext.getTenantCode();
        return inventoryWarningCacheService.topWarnings(tenantCode, 8);
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

    public InventoryImageRecognitionVO recognizeInboundImage(MultipartFile file) {
        validateRecognitionImage(file);
        BusinessAttachmentVO attachment = businessAttachmentService.upload(file, IMAGE_RECOGNITION_MODULE);
        InventoryImageRecognitionVO.Candidate candidate = buildRecognitionCandidate(attachment.getFileName());
        boolean hasCandidate = cleanText(candidate.getModelCode()) != null
                || candidate.getSpec() != null
                || candidate.getMeters() != null
                || cleanText(candidate.getBarcode()) != null;

        InventoryImageRecognitionVO vo = new InventoryImageRecognitionVO();
        vo.setFileName(attachment.getFileName());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setFileSize(attachment.getFileSize());
        vo.setStatus("NEED_CONFIRM");
        vo.setConfidence(candidate.getConfidence());
        vo.setCandidates(List.of(candidate));
        vo.setMessage(hasCandidate
                ? "图片已上传，系统已带出可疑字段，请人工核对后确认入库。"
                : "图片已上传。当前未接入正式 OCR，请人工补全型号、规格和米数后确认入库。");
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @CollectLog(module = "inventory", action = "cloth_in", bizType = "cloth", bizNo = "#p0.barcode", description = "网页端布匹入库")
    public InventoryInResultVO in(InventoryInRequest request) {
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
        cloth.setInType(InventoryInTypeEnum.normalizeInbound(request.getInType()));
        cloth.setIsBad(ClothQualityFlagEnum.NORMAL.getCode());
        cloth.setCustomFieldsJson(writeCustomFields(validateInventoryCustomFields(request.getCustomFields())));
        cloth.setVersion(INITIAL_VERSION);
        clothMapper.insert(cloth);

        saveRecord(cloth, InventoryOperateTypeEnum.IN.getCode(), request.getMeters(), userId, now);
        saveModelSpecIfAbsent(tenantCode, cloth.getModelCode(), cloth.getSpec());
        InventoryLabelTaskVO labelTask = createLabelTask(cloth, "网页端新增入库，请在小程序端蓝牙打印并粘贴布匹标签");
        invalidateDashboardCache(tenantCode);

        InventoryInResultVO result = new InventoryInResultVO();
        result.setCloth(toClothVO(cloth));
        result.setLabelTask(labelTask);
        return result;
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
                "一行代表一匹布；条码可为空，系统会自动生成；如果条码已存在，该行会导入失败，不会覆盖旧库存。",
                "剩余米数为空时默认等于总米数；状态为空时系统按剩余米数自动判断。",
                "可适配客户旧系统导出列名：货号、面料型号、物料编码、库存数量、现存米数、可用数量、卷号、匹号、批号、缸号等。",
                "导入是现有库存快照，不要求和系统模板完全一致；不能识别的列会自动忽略。"
        );
        excelUtil.writeTemplateToResponse(response,
                "外部库存导入字段说明",
                inventoryImportTemplateHeaders(currentTenantFieldLabels("inventory")),
                examples,
                notes,
                "外部库存导入字段说明.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    @CollectLog(module = "inventory", action = "inventory_import", bizType = "cloth", description = "网页端库存快照导入")
    public InventoryImportResultVO importInventory(MultipartFile file) {
        validateImportFile(file);
        List<List<String>> rows = readImportRows(file);
        Map<String, Set<String>> importHeaderAliases = tenantAwareImportHeaderAliases();
        int headerRowIndex = findHeaderRowIndex(rows, importHeaderAliases);
        if (headerRowIndex < 0) {
            throw new BusinessException("未识别到库存表头，请至少包含型号和库存米数/数量相关列");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(headerRowIndex), importHeaderAliases);
        if (!headerIndex.containsKey("modelCode")) {
            throw new BusinessException("导入文件缺少型号列，可使用：型号、货号、面料型号、物料编码");
        }
        if (!headerIndex.containsKey("totalMeters") && !headerIndex.containsKey("remainingMeters")) {
            throw new BusinessException("导入文件缺少米数列，可使用：总米数、库存米数、现存米数、数量、剩余米数");
        }

        InventoryImportResultVO result = new InventoryImportResultVO();
        Set<String> fileBarcodes = new HashSet<>();
        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            if (isRecognizableHeaderRow(row, importHeaderAliases)) {
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
                InventoryLabelTaskVO labelTask = createInventorySnapshot(importRow);
                if (labelTask != null && labelTask.getPrintTaskNo() != null) {
                    result.getLabelTasks().add(labelTask);
                    result.setPrintTaskCount(result.getLabelTasks().size());
                }
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

    private InventoryLabelTaskVO createInventorySnapshot(InventoryImportRow row) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        String barcode = row.barcode() == null ? generateBarcode(tenantCode) : row.barcode();

        if (existsBarcode(tenantCode, barcode)) {
            throw new BusinessException("条码已存在，外部库存导入按一行一匹布新增，不会覆盖旧库存：" + barcode);
        }

        Cloth cloth = new Cloth();
        cloth.setTenantCode(tenantCode);
        cloth.setBarcode(barcode);
        cloth.setCreateTime(now);
        cloth.setUpdateTime(now);
        cloth.setInOperatorId(userId);
        cloth.setInType(InventoryInTypeEnum.IMPORT_SNAPSHOT.getCode());
        cloth.setIsBad(ClothQualityFlagEnum.NORMAL.getCode());
        cloth.setVersion(INITIAL_VERSION);
        cloth.setModelCode(row.modelCode());
        cloth.setSpec(row.spec());
        cloth.setMeters(row.totalMeters());
        cloth.setTotalMeters(row.totalMeters());
        cloth.setRemainingMeters(row.remainingMeters());
        cloth.setStatus(row.status());
        cloth.setInTime(row.inTime() == null ? now : row.inTime());
        cloth.setCustomFieldsJson(writeCustomFields(validateInventoryCustomFields(row.customFields())));
        if (ClothInventoryStatusEnum.of(row.status()).isOutStock()) {
            cloth.setOutTime(now);
            cloth.setOutOperatorId(userId);
        }

        clothMapper.insert(cloth);
        saveRecord(cloth, InventoryOperateTypeEnum.EXTERNAL_IMPORT.getCode(), row.remainingMeters(), userId, now);
        saveModelSpecIfAbsent(tenantCode, row.modelCode(), row.spec());
        return createLabelTask(cloth, "网页端外部库存导入，请在小程序端蓝牙打印并粘贴布匹标签");
    }

    private InventoryLabelTaskVO createLabelTask(Cloth cloth, String reason) {
        InventoryLabelTaskVO labelTask = new InventoryLabelTaskVO();
        labelTask.setBarcode(cloth.getBarcode());
        labelTask.setModelCode(cloth.getModelCode());
        labelTask.setSpec(cloth.getSpec());
        labelTask.setMeters(cloth.getRemainingMeters());
        labelTask.setPrintReason(reason);
        String taskNo = printTaskService.createLabelTask(cloth.getBarcode(), labelTask, null, null, reason);
        labelTask.setPrintTaskNo(taskNo);
        return labelTask;
    }

    private ClothInventoryVO toClothVO(Cloth cloth) {
        ClothInventoryVO vo = new ClothInventoryVO();
        BeanUtils.copyProperties(cloth, vo);
        vo.setStatusName(statusName(cloth.getStatus()));
        vo.setCustomFields(readCustomFields(cloth.getCustomFieldsJson()));
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

    private int findHeaderRowIndex(List<List<String>> rows, Map<String, Set<String>> importHeaderAliases) {
        int limit = Math.min(rows.size(), 100);
        for (int i = 0; i < limit; i++) {
            Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(i), importHeaderAliases);
            boolean hasModel = headerIndex.containsKey("modelCode");
            boolean hasMeters = headerIndex.containsKey("totalMeters") || headerIndex.containsKey("remainingMeters");
            if (hasModel && hasMeters) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRecognizableHeaderRow(List<String> row, Map<String, Set<String>> importHeaderAliases) {
        Map<String, Integer> headerIndex = buildHeaderIndex(row, importHeaderAliases);
        boolean hasModel = headerIndex.containsKey("modelCode");
        boolean hasMeters = headerIndex.containsKey("totalMeters") || headerIndex.containsKey("remainingMeters");
        return hasModel && hasMeters;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headerRow, Map<String, Set<String>> importHeaderAliases) {
        Map<String, Integer> index = new HashMap<>();
        if (headerRow == null) {
            return index;
        }
        for (int i = 0; i < headerRow.size(); i++) {
            String column = resolveImportColumn(headerRow.get(i), importHeaderAliases);
            if (column != null) {
                index.putIfAbsent(column, i);
            }
        }
        return index;
    }

    private String resolveImportColumn(String header, Map<String, Set<String>> importHeaderAliases) {
        String normalized = normalizeHeader(header);
        if (normalized.isBlank()) {
            return null;
        }
        Map<String, Set<String>> aliases = importHeaderAliases == null ? IMPORT_HEADER_ALIASES : importHeaderAliases;
        for (Map.Entry<String, Set<String>> entry : aliases.entrySet()) {
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
                status,
                parseImportCustomFields(row, headerIndex)
        );
    }

    private Map<String, Object> parseImportCustomFields(List<String> row, Map<String, Integer> headerIndex) {
        if (headerIndex == null || headerIndex.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> customFields = new LinkedHashMap<>();
        headerIndex.forEach((fieldKey, index) -> {
            if (fieldKey == null || !fieldKey.startsWith("custom_")) {
                return;
            }
            String value = cleanText(getCell(row, index));
            if (value != null) {
                customFields.put(fieldKey, value);
            }
        });
        return customFields;
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

    private void validateRecognitionImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要识别的入库图片");
        }
        if (file.getSize() <= 0) {
            throw new BusinessException("图片内容为空，无法识别");
        }
        if (file.getSize() > MAX_IMAGE_RECOGNITION_BYTES) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!IMAGE_RECOGNITION_EXTENSIONS.contains(normalizedExtension)) {
            throw new BusinessException("图片识别入库仅支持 PNG、JPG、JPEG、WEBP 格式");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("上传文件不是有效图片");
        }
    }

    private InventoryImageRecognitionVO.Candidate buildRecognitionCandidate(String fileName) {
        String sourceText = stripExtension(fileName);
        InventoryImageRecognitionVO.Candidate candidate = new InventoryImageRecognitionVO.Candidate();
        candidate.setSourceText(sourceText);
        candidate.setBarcode(resolveBarcodeCandidate(sourceText));
        candidate.setModelCode(resolveModelCandidate(sourceText));
        candidate.setSpec(resolveDecimalCandidate(SPEC_FILENAME_PATTERN, sourceText));
        candidate.setMeters(resolveDecimalCandidate(METERS_FILENAME_PATTERN, sourceText));
        int recognizedFields = 0;
        if (cleanText(candidate.getBarcode()) != null) recognizedFields++;
        if (cleanText(candidate.getModelCode()) != null) recognizedFields++;
        if (candidate.getSpec() != null) recognizedFields++;
        if (candidate.getMeters() != null) recognizedFields++;
        candidate.setConfidence(BigDecimal.valueOf(Math.min(0.85D, 0.08D + recognizedFields * 0.18D)));
        return candidate;
    }

    private String stripExtension(String fileName) {
        String safeName = cleanText(fileName);
        if (safeName == null) {
            return "";
        }
        int dotIndex = safeName.lastIndexOf('.');
        return dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
    }

    private String resolveBarcodeCandidate(String sourceText) {
        if (sourceText == null) {
            return null;
        }
        for (String token : sourceText.split("[\\s_\\-]+")) {
            String cleaned = cleanText(token);
            if (cleaned != null && cleaned.length() >= 8 && cleaned.matches("[A-Za-z0-9]+")) {
                return cleaned;
            }
        }
        return null;
    }

    private String resolveModelCandidate(String sourceText) {
        if (sourceText == null) {
            return null;
        }
        for (String token : sourceText.split("[\\s_\\-]+")) {
            String cleaned = cleanText(token);
            if (cleaned == null || cleaned.matches("\\d+(\\.\\d+)?")) {
                continue;
            }
            String lower = cleaned.toLowerCase(Locale.ROOT);
            if (lower.contains("入库") || lower.contains("库存") || lower.contains("image") || lower.contains("photo")) {
                continue;
            }
            return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
        }
        return null;
    }

    private BigDecimal resolveDecimalCandidate(Pattern pattern, String sourceText) {
        if (sourceText == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(sourceText);
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(matcher.group(1));
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
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

    private Map<String, Object> validateInventoryCustomFields(Map<String, Object> input) {
        List<TenantFieldConfigVO> fieldConfigs = currentInventoryCustomFieldConfigs();
        Map<String, Object> source = input == null ? Map.of() : input;
        if (fieldConfigs.isEmpty()) {
            boolean hasCustomInput = source.entrySet().stream()
                    .anyMatch(entry -> entry.getKey() != null
                            && entry.getKey().startsWith("custom_")
                            && cleanText(String.valueOf(entry.getValue())) != null);
            if (hasCustomInput) {
                throw new BusinessException("当前租户未配置库存自定义字段，不能提交自定义字段");
            }
            return Map.of();
        }
        Map<String, TenantFieldConfigVO> configMap = new LinkedHashMap<>();
        for (TenantFieldConfigVO config : fieldConfigs) {
            if (config != null && config.getFieldKey() != null) {
                configMap.put(config.getFieldKey(), config);
            }
        }

        Map<String, Object> safeValues = new LinkedHashMap<>();
        for (TenantFieldConfigVO config : fieldConfigs) {
            String fieldKey = config.getFieldKey();
            Object rawValue = source.get(fieldKey);
            String value = rawValue == null ? null : cleanText(String.valueOf(rawValue));
            if (Boolean.TRUE.equals(config.getRequired()) && value == null) {
                throw new BusinessException("自定义字段不能为空：" + config.getFieldLabel());
            }
            if (value == null) {
                continue;
            }
            if (value.length() > MAX_CUSTOM_FIELD_VALUE_LENGTH) {
                throw new BusinessException("自定义字段内容过长：" + config.getFieldLabel());
            }
            safeValues.put(fieldKey, normalizeCustomFieldValue(config, value));
        }

        for (String key : source.keySet()) {
            if (key != null && key.startsWith("custom_") && !configMap.containsKey(key)) {
                throw new BusinessException("未配置的自定义字段不能提交：" + key);
            }
        }
        return safeValues;
    }

    private Object normalizeCustomFieldValue(TenantFieldConfigVO config, String value) {
        String fieldType = config.getFieldType() == null ? "text" : config.getFieldType();
        if ("number".equals(fieldType)) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException exception) {
                throw new BusinessException("自定义字段必须是数字：" + config.getFieldLabel());
            }
        }
        return value;
    }

    private List<TenantFieldConfigVO> currentInventoryCustomFieldConfigs() {
        try {
            return tenantFieldConfigService.currentCustomFieldConfigs("inventory");
        } catch (BusinessException ignored) {
            return List.of();
        }
    }

    private String writeCustomFields(Map<String, Object> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(customFields);
        } catch (Exception exception) {
            throw new BusinessException("自定义字段保存失败，请检查字段内容");
        }
    }

    private Map<String, Object> readCustomFields(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, CUSTOM_FIELD_TYPE_REFERENCE);
            return values == null ? Map.of() : values;
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private void invalidateDashboardCache(String tenantCode) {
        inventoryWarningCacheService.invalidate(tenantCode);
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
            Integer status,
            Map<String, Object> customFields
    ) {
    }
}
