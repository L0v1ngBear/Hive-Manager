package my.management.module.price.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
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
import my.management.module.price.model.vo.CustomerOptionVO;
import my.management.module.price.model.vo.CustomerOverrideVO;
import my.management.module.price.model.vo.ModelSpecOptionVO;
import my.management.module.price.model.vo.PriceChangeLogVO;
import my.management.module.price.model.vo.PriceDetailVO;
import my.management.module.price.model.vo.PriceSkuVO;
import my.management.module.price.model.vo.PriceStatsVO;
import my.management.module.price.model.vo.TierPriceVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PriceService {

    private static final int STATUS_EXPIRED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_SCHEDULED = 2;

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

    public Page<PriceSkuVO> page(PricePageRequest request) {
        LambdaQueryWrapper<PriceSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceSku::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(PriceSku::getIsDeleted, 0)
                .orderByDesc(PriceSku::getUpdateTime);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(PriceSku::getModelCode, request.getKeyword())
                    .or()
                    .like(PriceSku::getBatchNo, request.getKeyword())
                    .or()
                    .like(PriceSku::getSpec, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(PriceSku::getCategory, request.getCategory());
        }
        if (request.getStatus() != null) {
            wrapper.eq(PriceSku::getStatus, request.getStatus());
        }

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
                    .eq(PriceSku::getIsDeleted, 0)
                    .last("LIMIT 1"));
        }

        BigDecimal oldPrice = sku == null ? null : sku.getBasePrice();
        boolean created = sku == null;
        if (created) {
            sku = new PriceSku();
            sku.setTenantCode(tenantCode);
            sku.setIsDeleted(0);
        } else if (!Objects.equals(sku.getTenantCode(), tenantCode)) {
            throw new BusinessException("价格记录不存在");
        }

        sku.setModelCode(request.getModelCode());
        sku.setBatchNo(request.getBatchNo());
        sku.setCategory(request.getCategory());
        sku.setSpec(request.getSpec());
        sku.setBasePrice(request.getBasePrice());
        sku.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "CNY");
        sku.setEffectiveDate(request.getEffectiveDate());
        sku.setStatus(calcStatus(request.getEffectiveDate()));
        sku.setImageUrl(request.getImageUrl());
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
                        .eq(PriceTierPrice::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(PriceTierPrice::getIsDeleted, 0)
                        .orderByAsc(PriceTierPrice::getTierCode))
                .stream()
                .map(item -> toTierVO(item, sku.getBasePrice()))
                .toList());
        detail.setOverrides(priceCustomerOverrideMapper.selectList(new LambdaQueryWrapper<PriceCustomerOverride>()
                        .eq(PriceCustomerOverride::getSkuId, id)
                        .eq(PriceCustomerOverride::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(PriceCustomerOverride::getIsDeleted, 0)
                        .orderByDesc(PriceCustomerOverride::getId))
                .stream()
                .map(this::toOverrideVO)
                .toList());
        detail.setLogs(priceChangeLogMapper.selectList(new LambdaQueryWrapper<PriceChangeLog>()
                        .eq(PriceChangeLog::getSkuId, id)
                        .eq(PriceChangeLog::getTenantCode, TenantPermissionContext.getTenantCode())
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
        sku.setIsDeleted(1);
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

    public List<String> categories() {
        return priceSkuMapper.selectCategories(TenantPermissionContext.getTenantCode());
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
            entity.setIsDeleted(0);
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
            entity.setIsDeleted(0);
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
        if (sku == null || !Objects.equals(sku.getTenantCode(), TenantPermissionContext.getTenantCode()) || Integer.valueOf(1).equals(sku.getIsDeleted())) {
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
        return effectiveDate != null && effectiveDate.isAfter(LocalDate.now()) ? STATUS_SCHEDULED : STATUS_ACTIVE;
    }

    private String statusLabel(Integer status) {
        if (Integer.valueOf(STATUS_ACTIVE).equals(status)) {
            return "生效中";
        }
        if (Integer.valueOf(STATUS_SCHEDULED).equals(status)) {
            return "计划中";
        }
        if (Integer.valueOf(STATUS_EXPIRED).equals(status)) {
            return "已过期";
        }
        return "未知";
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
}