package my.management.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.ProductionOrderStatusLogMapper;
import my.management.module.order.mapper.SalesOrderDetailMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.model.dto.ProductionOrderPageRequest;
import my.management.module.order.model.dto.ProductionOrderUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.ProductionOrderStatusLog;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.entity.SalesOrderDetail;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单管理服务，统一承接销售订单与生产订单的查询和维护。
 */
@Service
public class OrderService {

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private SalesOrderDetailMapper salesOrderDetailMapper;

    @Resource
    private ProductionOrderMapper productionOrderMapper;

    @Resource
    private ProductionOrderStatusLogMapper productionOrderStatusLogMapper;

    public Page<SalesOrderPageVO> pageSalesOrders(SalesOrderPageRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Page<SalesOrder> page = new Page<>(safePage(request.getPageNum()), safeSize(request.getPageSize()));

        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .orderByDesc(SalesOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(SalesOrder::getStatus, request.getStatus().trim());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(SalesOrder::getOrderId, keyword)
                    .or().like(SalesOrder::getCustomerName, keyword)
                    .or().like(SalesOrder::getCustomerPhone, keyword)
                    .or().like(SalesOrder::getGoodsDesc, keyword));
        }

        Page<SalesOrder> source = salesOrderMapper.selectPage(page, wrapper);
        List<SalesOrder> orders = source.getRecords();
        Map<String, Integer> detailCountMap = buildSalesDetailCountMap(orders);

        List<SalesOrderPageVO> records = orders.stream().map(order -> {
            SalesOrderPageVO vo = new SalesOrderPageVO();
            BeanUtils.copyProperties(order, vo);
            vo.setDetailCount(detailCountMap.getOrDefault(order.getOrderId(), 0));
            return vo;
        }).toList();

        Page<SalesOrderPageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    public SalesOrderDetailVO getSalesOrderDetail(String orderId) {
        SalesOrder order = findSalesOrder(orderId);
        List<SalesOrderDetail> details = salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId)
                .orderByAsc(SalesOrderDetail::getId));

        SalesOrderDetailVO vo = new SalesOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setItems(details.stream().map(detail -> {
            SalesOrderDetailVO.ItemVO itemVO = new SalesOrderDetailVO.ItemVO();
            BeanUtils.copyProperties(detail, itemVO);
            return itemVO;
        }).toList());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSalesOrder(String orderId, SalesOrderUpdateRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        if (StringUtils.hasText(request.getStatus())) {
            order.setStatus(request.getStatus().trim());
        }
        if (request.getDeliveryDate() != null) {
            order.setDeliveryDate(blankToNull(request.getDeliveryDate()));
        }
        if (request.getExpressCompany() != null) {
            order.setExpressCompany(blankToNull(request.getExpressCompany()));
        }
        if (request.getExpressNo() != null) {
            order.setExpressNo(blankToNull(request.getExpressNo()));
        }
        if (request.getRemark() != null) {
            order.setRemark(blankToNull(request.getRemark()));
        }
        salesOrderMapper.updateById(order);
    }

    public Page<ProductionOrderPageVO> pageProductionOrders(ProductionOrderPageRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Page<ProductionOrder> page = new Page<>(safePage(request.getPageNum()), safeSize(request.getPageSize()));

        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, tenantCode)
                .orderByDesc(ProductionOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ProductionOrder::getStatus, request.getStatus().trim());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(ProductionOrder::getOrderId, keyword)
                    .or().like(ProductionOrder::getSalesOrderId, keyword)
                    .or().like(ProductionOrder::getCustomerName, keyword)
                    .or().like(ProductionOrder::getProjectName, keyword)
                    .or().like(ProductionOrder::getModelCode, keyword));
        }

        Page<ProductionOrder> source = productionOrderMapper.selectPage(page, wrapper);
        List<ProductionOrderPageVO> records = source.getRecords().stream().map(order -> {
            ProductionOrderPageVO vo = new ProductionOrderPageVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).toList();

        Page<ProductionOrderPageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    public ProductionOrderDetailVO getProductionOrderDetail(String orderId) {
        ProductionOrder order = findProductionOrder(orderId);
        List<ProductionOrderStatusLogVO> logs = listProductionLogs(orderId);

        ProductionOrderDetailVO vo = new ProductionOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setLogs(logs);
        return vo;
    }

    public List<ProductionOrderStatusLogVO> listProductionLogs(String orderId) {
        findProductionOrder(orderId);
        return productionOrderStatusLogMapper.selectList(new LambdaQueryWrapper<ProductionOrderStatusLog>()
                        .eq(ProductionOrderStatusLog::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(ProductionOrderStatusLog::getOrderId, orderId)
                        .orderByDesc(ProductionOrderStatusLog::getCreateTime))
                .stream()
                .map(log -> {
                    ProductionOrderStatusLogVO vo = new ProductionOrderStatusLogVO();
                    BeanUtils.copyProperties(log, vo);
                    return vo;
                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProductionOrder(String orderId, ProductionOrderUpdateRequest request) {
        ProductionOrder order = findProductionOrder(orderId);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        boolean changed = false;

        if (StringUtils.hasText(request.getStatus()) && !Objects.equals(order.getStatus(), request.getStatus().trim())) {
            order.setStatus(request.getStatus().trim());
            changed = true;
        }
        if (request.getProcess() != null && !Objects.equals(order.getProcess(), request.getProcess())) {
            order.setProcess(request.getProcess());
            changed = true;
        }

        productionOrderMapper.updateById(order);

        if (changed || StringUtils.hasText(request.getRemark())) {
            ProductionOrderStatusLog log = new ProductionOrderStatusLog();
            log.setTenantCode(order.getTenantCode());
            log.setOrderId(order.getOrderId());
            log.setOldStatus(oldStatusText);
            log.setNewStatus(buildProductionStateText(order.getStatus(), order.getProcess()));
            log.setOperateType(resolveOperateType(changed, request.getProcess()));
            log.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "管理端更新订单状态");
            log.setOperator(String.valueOf(TenantPermissionContext.getUserId()));
            log.setCreateTime(LocalDateTime.now());
            productionOrderStatusLogMapper.insert(log);
        }
    }

    public Map<String, Object> checkOrderTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("salesOrderTableReady", salesOrderMapper.selectCount(null) >= 0);
        result.put("salesOrderDetailTableReady", salesOrderDetailMapper.selectCount(null) >= 0);
        result.put("productionOrderTableReady", productionOrderMapper.selectCount(null) >= 0);
        result.put("productionOrderLogTableReady", productionOrderStatusLogMapper.selectCount(null) >= 0);
        result.put("checkedAt", LocalDateTime.now());
        return result;
    }

    private SalesOrder findSalesOrder(String orderId) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SalesOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("sales order not found");
        }
        return order;
    }

    private ProductionOrder findProductionOrder(String orderId) {
        ProductionOrder order = productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(ProductionOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("production order not found");
        }
        return order;
    }

    private Map<String, Integer> buildSalesDetailCountMap(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> orderIds = orders.stream().map(SalesOrder::getOrderId).toList();
        return salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                        .in(SalesOrderDetail::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(SalesOrderDetail::getOrderId, LinkedHashMap::new, Collectors.summingInt(item -> 1)));
    }

    private String buildProductionStateText(String status, Integer process) {
        if (!StringUtils.hasText(status)) {
            return "未设置";
        }
        if (process == null) {
            return status;
        }
        return status + " / 工序 " + process;
    }

    private String resolveOperateType(boolean changed, Integer process) {
        if (process != null) {
            return "process_change";
        }
        return changed ? "status_change" : "remark";
    }

    private long safePage(long pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    private long safeSize(long pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
