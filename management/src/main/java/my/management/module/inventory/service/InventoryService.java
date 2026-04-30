package my.management.module.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.annotation.CollectLog;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.hive.common.utils.RedisCacheHelper;
import my.management.module.inventory.mapper.ClothMapper;
import my.management.module.inventory.mapper.ClothModelSpecMapper;
import my.management.module.inventory.mapper.InventoryRecordMapper;
import my.management.module.inventory.model.dto.InventoryInRequest;
import my.management.module.inventory.model.dto.InventoryOutRequest;
import my.management.module.inventory.model.dto.InventoryPageRequest;
import my.management.module.inventory.model.entity.Cloth;
import my.management.module.inventory.model.entity.ClothModelSpec;
import my.management.module.inventory.model.entity.InventoryRecord;
import my.management.module.inventory.model.vo.ClothInventoryVO;
import my.management.module.inventory.model.vo.InventoryModelOptionVO;
import my.management.module.inventory.model.vo.InventoryRecordVO;
import my.management.module.inventory.model.vo.InventorySummaryVO;
import my.management.module.inventory.model.vo.InventoryTrendVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        summary.setTodayInMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, 0, startTime, endTime)));
        summary.setTodayOutMeters(nvl(inventoryRecordMapper.sumOperateMeters(tenantCode, 1, startTime, endTime)));
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
                .peek(item -> item.setOperateTypeName(item.getOperateType() != null && item.getOperateType() == 1 ? "出库" : "入库"))
                .toList();
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
        cloth.setStatus(0);
        cloth.setInTime(now);
        cloth.setInOperatorId(userId);
        cloth.setInType(request.getInType() == null || request.getInType().isBlank() ? "manual" : request.getInType().trim());
        cloth.setIsBad(0);
        cloth.setVersion(0);
        clothMapper.insert(cloth);

        saveRecord(cloth, 0, request.getMeters(), userId, now);
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
                .eq(Cloth::getBarcode, request.getBarcode().trim()));
        if (cloth == null) {
            throw new BusinessException("未找到该条码库存");
        }
        if (cloth.getRemainingMeters() == null || cloth.getRemainingMeters().compareTo(request.getMeters()) < 0) {
            throw new BusinessException("库存米数不足，无法出库");
        }

        BigDecimal remaining = cloth.getRemainingMeters().subtract(request.getMeters());
        cloth.setRemainingMeters(remaining);
        cloth.setStatus(remaining.compareTo(BigDecimal.ZERO) == 0 ? 1 : 2);
        cloth.setOutTime(now);
        cloth.setOutOperatorId(userId);
        cloth.setUpdateTime(now);
        clothMapper.updateById(cloth);

        saveRecord(cloth, 1, request.getMeters(), userId, now);
        invalidateDashboardCache(tenantCode);
    }

    private ClothInventoryVO toClothVO(Cloth cloth) {
        ClothInventoryVO vo = new ClothInventoryVO();
        BeanUtils.copyProperties(cloth, vo);
        vo.setStatusName(statusName(cloth.getStatus()));
        return vo;
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
                .eq(Cloth::getBarcode, barcode));
        return count != null && count > 0;
    }

    private String statusName(Integer status) {
        if (status == null || status == 0) {
            return "在库";
        }
        if (status == 1) {
            return "已出库";
        }
        if (status == 2) {
            return "部分出库";
        }
        return "未知";
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
}
