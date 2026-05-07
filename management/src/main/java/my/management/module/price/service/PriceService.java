package my.management.module.price.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.DeleteFlagEnum;
import my.management.common.utils.ExcelUtil;
import my.management.common.vo.ImportResultVO;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.model.entity.Customer;
import my.management.module.price.mapper.ClothModelSpecViewMapper;
import my.management.module.price.mapper.PriceChangeLogMapper;
import my.management.module.price.mapper.PriceCustomerOverrideMapper;
import my.management.module.price.mapper.PriceSkuMapper;
import my.management.module.price.mapper.PriceTierPriceMapper;
import my.management.module.price.model.dto.CustomerOverrideRequest;
import my.management.module.price.model.dto.PricePageRequest;
import my.management.module.price.model.dto.PricePublishRequest;
import my.management.module.price.model.dto.TierPriceRequest;
import my.management.module.price.model.entity.PriceChangeLog;
import my.management.module.price.model.entity.PriceCustomerOverride;
import my.management.module.price.model.entity.PriceSku;
import my.management.module.price.model.entity.PriceTierPrice;
import my.management.module.price.model.enums.PriceSkuStatusEnum;
import my.management.module.price.model.vo.CustomerOptionVO;
import my.management.module.price.model.vo.CustomerOverrideVO;
import my.management.module.price.model.vo.ModelSpecOptionVO;
import my.management.module.price.model.vo.PriceChangeLogVO;
import my.management.module.price.model.vo.PriceDetailVO;
import my.management.module.price.model.vo.PriceSkuVO;
import my.management.module.price.model.vo.PriceStatsVO;
import my.management.module.price.model.vo.TierPriceVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
/**
 * PriceService 属于管理端后端价格模块，实现核心业务编排与规则逻辑。
 */
@Service
public class PriceService {

    private static final int PRICE_IMPORT_COLUMN_COUNT = 7;
    private static final int MAX_PRICE_IMPORT_ROWS = 10000;
    private static final long MAX_IMPORT_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_MODEL_CODE_LENGTH = 128;
    private static final int MAX_BATCH_NO_LENGTH = 64;
    private static final int MAX_SPEC_LENGTH = 255;
    private static final int MAX_CURRENCY_LENGTH = 16;
    private static final int MAX_REMARK_LENGTH = 500;
    private static final BigDecimal MAX_BASE_PRICE = new BigDecimal("9999999999.99");
    private static final List<String> PRICE_IMPORT_HEADERS = List.of("面料型号", "批号", "规格", "基准价", "币种", "生效日期", "备注");

    @Resource
    private PriceSkuMapper priceSkuMapper;

    @Resource
    private PriceTierPriceMapper priceTierPriceMapper;

    @Resource
    private PriceCustomerOverrideMapper priceCustomerOverrideMapper;

    @Resource
    private PriceChangeLogMapper priceChangeLogMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ClothModelSpecViewMapper clothModelSpecViewMapper;

    @Resource
    private ExcelUtil excelUtil;

    public Page<PriceSkuVO> page(PricePageRequest request) {
        LambdaQueryWrapper<PriceSku> wrapper = buildQueryWrapper(request);
        Page<PriceSku> entityPage = priceSkuMapper.selectPage(new Page<>(safePage(request.getPage()), safeSize(request.getSize())), wrapper);
        Page<PriceSkuVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toSkuVO).toList());
        return voPage;
    }

    public PriceStatsVO stats() {
        PriceStatsVO stats = priceSkuMapper.selectStats(TenantPermissionContext.getTenantCode());
        if (stats == null) {
            stats = new PriceStatsVO();
        }
        stats.setSkuCount(nvl(stats.getSkuCount()));
        stats.setAveragePrice(nvl(stats.getAveragePrice()));
        stats.setPendingCount(nvl(stats.getPendingCount()));
        Long overrideCount = priceCustomerOverrideMapper.selectCount(new LambdaQueryWrapper<PriceCustomerOverride>()
                .eq(PriceCustomerOverride::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(PriceCustomerOverride::getIsDeleted, 0));
        stats.setOverrideCount(nvl(overrideCount));
        return stats;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(@Valid PricePublishRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        PriceSku sku = request.getId() == null ? null : priceSkuMapper.selectById(request.getId());
        if (sku == null) {
            sku = priceSkuMapper.selectOne(new LambdaQueryWrapper<PriceSku>()
                    .eq(PriceSku::getTenantCode, tenantCode)
                    .eq(PriceSku::getModelCode, request.getModelCode())
                    .eq(PriceSku::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                    .last("LIMIT 1"));
        }

        BigDecimal oldPrice = sku == null ? null : sku.getBasePrice();
        boolean created = sku == null;
        if (created) {
            sku = new PriceSku();
            sku.setTenantCode(tenantCode);
            sku.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        } else if (!Objects.equals(sku.getTenantCode(), tenantCode)) {
            throw new BusinessException("价格记录不存在");
        }

        sku.setModelCode(request.getModelCode());
        sku.setBatchNo(request.getBatchNo());
        // Category is no longer maintained in the price workflow, so new writes clear it.
        sku.setCategory(null);
        sku.setSpec(request.getSpec());
        sku.setBasePrice(request.getBasePrice());
        sku.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "CNY");
        sku.setEffectiveDate(request.getEffectiveDate());
        sku.setStatus(calcStatus(request.getEffectiveDate()));
        sku.setRemark(request.getRemark());

        if (created) {
            priceSkuMapper.insert(sku);
        } else {
            priceSkuMapper.updateById(sku);
            clearMatrix(sku.getId());
        }

        saveTierPrices(sku.getId(), request.getTierPrices(), sku.getBasePrice());
        saveOverrides(sku.getId(), request.getOverrides());
        insertLog(sku, oldPrice, request.getBasePrice(), created ? "创建价格" : "调整价格");
        return sku.getId();
    }

    public PriceDetailVO detail(Long id) {
        PriceSku sku = requireSku(id);
        PriceDetailVO detail = new PriceDetailVO();
        BeanUtils.copyProperties(toSkuVO(sku), detail);
        detail.setTierPrices(priceTierPriceMapper.selectList(new LambdaQueryWrapper<PriceTierPrice>()
                        .eq(PriceTierPrice::getSkuId, id)
                        .eq(PriceTierPrice::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(PriceTierPrice::getTierCode))
                .stream()
                .map(item -> toTierVO(item, sku.getBasePrice()))
                .toList());
        detail.setOverrides(priceCustomerOverrideMapper.selectList(new LambdaQueryWrapper<PriceCustomerOverride>()
                        .eq(PriceCustomerOverride::getSkuId, id)
                        .eq(PriceCustomerOverride::getIsDeleted, DeleteFlagEnum.NORMAL.getCode())
                        .orderByDesc(PriceCustomerOverride::getId))
                .stream()
                .map(this::toOverrideVO)
                .toList());
        detail.setLogs(priceChangeLogMapper.selectList(new LambdaQueryWrapper<PriceChangeLog>()
                        .eq(PriceChangeLog::getSkuId, id)
                        .orderByDesc(PriceChangeLog::getCreateTime)
                        .last("LIMIT 20"))
                .stream()
                .map(this::toLogVO)
                .toList());
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PriceSku sku = requireSku(id);
        sku.setIsDeleted(DeleteFlagEnum.DELETED.getCode());
        priceSkuMapper.updateById(sku);
    }

    public List<CustomerOptionVO> customerOptions(String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, TenantPermissionContext.getTenantCode())
                .orderByDesc(Customer::getId)
                .last("LIMIT 50");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Customer::getCustomerName, keyword);
        }
        return customerMapper.selectList(wrapper).stream().map(customer -> {
            CustomerOptionVO vo = new CustomerOptionVO();
            vo.setId(customer.getId());
            vo.setCustomerName(customer.getCustomerName());
            return vo;
        }).toList();
    }

    public List<ModelSpecOptionVO> modelOptions(String keyword) {
        return clothModelSpecViewMapper.searchModelSpec(TenantPermissionContext.getTenantCode(), keyword, 50);
    }

    public void exportExcel(PricePageRequest request, HttpServletResponse response) {
        List<String> headers = List.of("面料型号", "批号", "规格", "基准价", "币种", "生效日期", "状态", "备注");
        List<List<String>> rows = priceSkuMapper.selectList(buildQueryWrapper(request)).stream()
                .map(this::toSkuVO)
                .map(item -> List.of(
                        excelUtil.stringify(item.getModelCode()),
                        excelUtil.stringify(item.getBatchNo()),
                        excelUtil.stringify(item.getSpec()),
                        excelUtil.stringify(item.getBasePrice()),
                        excelUtil.stringify(item.getCurrency()),
                        excelUtil.stringify(item.getEffectiveDate()),
                        excelUtil.stringify(item.getStatusLabel()),
                        excelUtil.stringify(item.getRemark())
                ))
                .toList();
        excelUtil.writeRowsToResponse(response,
                "价格列表",
                headers,
                rows,
                "价格列表.xlsx");
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        List<String> headers = List.of("面料型号", "批号", "规格", "基准价", "币种", "生效日期", "备注");
        List<List<String>> examples = List.of(
                List.of("978-1-56915-435-9", "BATCH-202604", "0.00", "32.50", "CNY", LocalDate.now().toString(), "导入示例"),
                List.of("978-0-392-85262-3", "BATCH-202604", "1.00", "45.80", "CNY", LocalDate.now().plusDays(1).toString(), "计划生效价格")
        );
        List<String> notes = List.of(
                "仅支持 .xlsx 文件导入。",
                "必填列：面料型号、基准价。",
                "生效日期格式：yyyy-MM-dd。为空时默认当天。",
                "币种为空时默认 CNY。",
                "导入时会复用价格发布逻辑，同型号将自动更新当前有效价格。",
                "客户特价、等级价暂不在批量导入模板内，后续由业务自行补充。"
        );
        excelUtil.writeTemplateToResponse(response,
                "价格导入模板",
                headers,
                examples,
                notes,
                "价格导入模板.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importPrices(MultipartFile file) {
        validateImportFile(file);
        ImportResultVO result = new ImportResultVO();
        Set<String> importedModelCodes = new HashSet<>();
        try (var inputStream = file.getInputStream(); var workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            validateImportHeader(sheet.getRow(0), PRICE_IMPORT_HEADERS);
            validateImportDataRows(sheet, PRICE_IMPORT_COLUMN_COUNT, MAX_PRICE_IMPORT_ROWS);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, PRICE_IMPORT_COLUMN_COUNT)) {
                    continue;
                }
                result.setTotalCount(result.getTotalCount() + 1);
                try {
                    PricePublishRequest request = buildImportRequest(row);
                    if (!importedModelCodes.add(request.getModelCode())) {
                        throw new BusinessException("面料型号在导入文件中重复：" + request.getModelCode());
                    }
                    publish(request);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    if (result.getFailMessages().size() < 20) {
                        result.getFailMessages().add("第 " + (i + 1) + " 行：" + ex.getMessage());
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("读取价格导入文件失败");
        } catch (Exception e) {
            throw new BusinessException("价格导入文件格式不正确，请使用系统下载的 .xlsx 模板");
        }
        return result;
    }

    private void saveTierPrices(Long skuId, List<TierPriceRequest> tierPrices, BigDecimal basePrice) {
        List<TierPriceRequest> rows = CollectionUtils.isEmpty(tierPrices) ? defaultTiers() : tierPrices;
        for (TierPriceRequest row : rows) {
            PriceTierPrice entity = new PriceTierPrice();
            entity.setTenantCode(TenantPermissionContext.getTenantCode());
            entity.setSkuId(skuId);
            entity.setTierCode(row.getTierCode());
            entity.setTierName(row.getTierName());
            entity.setFixedPrice(row.getFixedPrice());
            entity.setDiscountRate(row.getDiscountRate() == null ? BigDecimal.valueOf(100) : row.getDiscountRate());
            entity.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            if (entity.getFixedPrice() == null && basePrice != null && entity.getDiscountRate() != null) {
                entity.setFixedPrice(basePrice.multiply(entity.getDiscountRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            priceTierPriceMapper.insert(entity);
        }
    }

    private void saveOverrides(Long skuId, List<CustomerOverrideRequest> overrides) {
        if (CollectionUtils.isEmpty(overrides)) {
            return;
        }
        for (CustomerOverrideRequest row : overrides) {
            if (row.getCustomerId() == null || row.getPrice() == null) {
                continue;
            }
            Customer customer = customerMapper.selectById(row.getCustomerId());
            PriceCustomerOverride entity = new PriceCustomerOverride();
            entity.setTenantCode(TenantPermissionContext.getTenantCode());
            entity.setSkuId(skuId);
            entity.setCustomerId(row.getCustomerId());
            entity.setCustomerName(customer == null ? row.getCustomerName() : customer.getCustomerName());
            entity.setPrice(row.getPrice());
            entity.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
            priceCustomerOverrideMapper.insert(entity);
        }
    }

    private void clearMatrix(Long skuId) {
        priceTierPriceMapper.delete(new LambdaQueryWrapper<PriceTierPrice>().eq(PriceTierPrice::getSkuId, skuId));
        priceCustomerOverrideMapper.delete(new LambdaQueryWrapper<PriceCustomerOverride>().eq(PriceCustomerOverride::getSkuId, skuId));
    }

    private void insertLog(PriceSku sku, BigDecimal oldPrice, BigDecimal newPrice, String remark) {
        PriceChangeLog log = new PriceChangeLog();
        log.setTenantCode(TenantPermissionContext.getTenantCode());
        log.setSkuId(sku.getId());
        log.setModelCode(sku.getModelCode());
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setOperatorUserId(TenantPermissionContext.getUserId());
        log.setRemark(remark);
        priceChangeLogMapper.insert(log);
    }

    private PriceSku requireSku(Long id) {
        PriceSku sku = priceSkuMapper.selectById(id);
        if (sku == null || !Objects.equals(sku.getTenantCode(), TenantPermissionContext.getTenantCode()) || DeleteFlagEnum.isDeleted(sku.getIsDeleted())) {
            throw new BusinessException("价格记录不存在");
        }
        return sku;
    }

    private PriceSkuVO toSkuVO(PriceSku sku) {
        PriceSkuVO vo = new PriceSkuVO();
        BeanUtils.copyProperties(sku, vo);
        vo.setStatusLabel(statusLabel(sku.getStatus()));
        return vo;
    }

    private TierPriceVO toTierVO(PriceTierPrice item, BigDecimal basePrice) {
        TierPriceVO vo = new TierPriceVO();
        BeanUtils.copyProperties(item, vo);
        vo.setFinalPrice(item.getFixedPrice() != null ? item.getFixedPrice() : basePrice);
        return vo;
    }

    private CustomerOverrideVO toOverrideVO(PriceCustomerOverride item) {
        CustomerOverrideVO vo = new CustomerOverrideVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private PriceChangeLogVO toLogVO(PriceChangeLog item) {
        PriceChangeLogVO vo = new PriceChangeLogVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private int calcStatus(LocalDate effectiveDate) {
        return PriceSkuStatusEnum.fromEffectiveDate(effectiveDate).getCode();
    }

    private String statusLabel(Integer status) {
        if (PriceSkuStatusEnum.SCHEDULED.getCode().equals(status)) {
            return "计划中";
        }
        return PriceSkuStatusEnum.of(status).getLabel();
    }

    private List<TierPriceRequest> defaultTiers() {
        TierPriceRequest strategic = new TierPriceRequest();
        strategic.setTierCode("T1");
        strategic.setTierName("战略客户");
        strategic.setDiscountRate(BigDecimal.valueOf(90));

        TierPriceRequest bulk = new TierPriceRequest();
        bulk.setTierCode("T2");
        bulk.setTierName("大宗采购");
        bulk.setDiscountRate(BigDecimal.valueOf(95));

        TierPriceRequest standard = new TierPriceRequest();
        standard.setTierCode("T3");
        standard.setTierName("标准客户");
        standard.setDiscountRate(BigDecimal.valueOf(100));
        return List.of(strategic, bulk, standard);
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LambdaQueryWrapper<PriceSku> buildQueryWrapper(PricePageRequest request) {
        LambdaQueryWrapper<PriceSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceSku::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(PriceSku::getIsDeleted, 0)
                .orderByDesc(PriceSku::getUpdateTime);
        // 分类字段已下线，这里只保留关键词和状态两个有效筛选入口。
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(PriceSku::getModelCode, request.getKeyword())
                    .or()
                    .like(PriceSku::getBatchNo, request.getKeyword())
                    .or()
                    .like(PriceSku::getSpec, request.getKeyword()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(PriceSku::getStatus, request.getStatus());
        }
        return wrapper;
    }

    private boolean isEmptyRow(Row row, int cellCount) {
        for (int i = 0; i < cellCount; i++) {
            if (StringUtils.hasText(excelUtil.readString(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private PricePublishRequest buildImportRequest(Row row) {
        String modelCode = requireImportText("面料型号", excelUtil.readString(row.getCell(0)), MAX_MODEL_CODE_LENGTH);
        String batchNo = optionalImportText("批号", excelUtil.readString(row.getCell(1)), MAX_BATCH_NO_LENGTH);
        String spec = optionalImportText("规格", excelUtil.readString(row.getCell(2)), MAX_SPEC_LENGTH);
        BigDecimal basePrice = parseImportPrice(excelUtil.readString(row.getCell(3)));
        String currency = normalizeImportCurrency(excelUtil.readString(row.getCell(4)));
        LocalDate effectiveDate = readImportDate(row, 5, "生效日期", LocalDate.now());
        String remark = optionalImportText("备注", excelUtil.readString(row.getCell(6)), MAX_REMARK_LENGTH);

        PricePublishRequest request = new PricePublishRequest();
        request.setModelCode(modelCode);
        request.setBatchNo(batchNo);
        request.setSpec(spec);
        request.setBasePrice(basePrice);
        request.setCurrency(currency);
        request.setEffectiveDate(effectiveDate);
        request.setRemark(remark);
        return request;
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择要导入的 Excel 文件");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE_BYTES) {
            throw new BusinessException("导入文件不能超过 20MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.trim().toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式，请先下载系统导入模板");
        }
    }

    private void validateImportHeader(Row headerRow, List<String> expectedHeaders) {
        if (headerRow == null) {
            throw new BusinessException("导入文件缺少表头，请下载最新模板");
        }
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String actual = excelUtil.readString(headerRow.getCell(i));
            String expected = expectedHeaders.get(i);
            if (!expected.equals(actual)) {
                throw new BusinessException("导入表头不匹配，第 " + (i + 1) + " 列应为：" + expected);
            }
        }
    }

    private void validateImportDataRows(Sheet sheet, int cellCount, int maxRows) {
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
            if (dataRows > maxRows) {
                throw new BusinessException("单次最多导入 " + maxRows + " 行，请拆分文件后重试");
            }
        }
        if (dataRows == 0) {
            throw new BusinessException("导入文件没有有效数据行");
        }
    }

    private String requireImportText(String fieldName, String value, int maxLength) {
        String normalized = optionalImportText(fieldName, value, maxLength);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String optionalImportText(String fieldName, String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private BigDecimal parseImportPrice(String value) {
        String normalized = requireImportText("基准价", value, 32);
        BigDecimal price;
        try {
            price = new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new BusinessException("基准价必须是合法数字");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("基准价必须大于 0");
        }
        if (price.compareTo(MAX_BASE_PRICE) > 0) {
            throw new BusinessException("基准价不能超过 " + MAX_BASE_PRICE);
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("基准价最多保留 2 位小数");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeImportCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return "CNY";
        }
        String normalized = optionalImportText("币种", currency, MAX_CURRENCY_LENGTH).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new BusinessException("币种必须是 3 位大写字母，例如 CNY");
        }
        return normalized;
    }

    private LocalDate readImportDate(Row row, int cellIndex, String fieldName, LocalDate fallback) {
        try {
            LocalDate value = excelUtil.readLocalDate(row.getCell(cellIndex));
            return value == null ? fallback : value;
        } catch (DateTimeParseException ex) {
            throw new BusinessException(fieldName + "格式必须为 yyyy-MM-dd");
        }
    }
}
