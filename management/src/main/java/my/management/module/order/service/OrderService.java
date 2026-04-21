package my.management.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.entity.Customer;
import my.management.module.customer.model.entity.CustomerProject;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.ProductionOrderStatusLogMapper;
import my.management.module.order.mapper.SalesOrderDetailMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.mapper.SalesOrderStatusLogMapper;
import my.management.module.order.model.dto.ProductionOrderPageRequest;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.dto.ProductionOrderUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.ProductionOrderStatusLog;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.entity.SalesOrderDetail;
import my.management.module.order.model.entity.SalesOrderStatusLog;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import my.management.module.order.model.vo.SalesOrderStatusLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单管理服务，统一承接销售订单与生产订单的查询、创建和完整维护。
 */
@Service
public class OrderService {

    private static final String DEFAULT_SALES_STATUS = "pending_confirm";
    private static final String DEFAULT_PRODUCTION_STATUS = "pending_confirm";
    private static final String STATUS_SHIPPED = "shipped";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_PENDING_CONFIRM = "pending_confirm";
    private static final String STATUS_PENDING_SHIP = "pending_ship";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private SalesOrderDetailMapper salesOrderDetailMapper;

    @Resource
    private ProductionOrderMapper productionOrderMapper;

    @Resource
    private ProductionOrderStatusLogMapper productionOrderStatusLogMapper;

    @Resource
    private SalesOrderStatusLogMapper salesOrderStatusLogMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerProjectMapper customerProjectMapper;

    @Resource
    private EmployeeMapper employeeMapper;

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
                    .or().like(SalesOrder::getProjectName, keyword)
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
        vo.setLogs(listSalesLogs(orderId));
        return vo;
    }

    public List<SalesOrderStatusLogVO> listSalesLogs(String orderId) {
        findSalesOrder(orderId);
        return salesOrderStatusLogMapper.selectList(new LambdaQueryWrapper<SalesOrderStatusLog>()
                        .eq(SalesOrderStatusLog::getTenantCode, TenantPermissionContext.getTenantCode())
                        .eq(SalesOrderStatusLog::getOrderId, orderId)
                        .orderByDesc(SalesOrderStatusLog::getCreateTime))
                .stream()
                .map(log -> {
                    SalesOrderStatusLogVO vo = new SalesOrderStatusLogVO();
                    BeanUtils.copyProperties(log, vo);
                    return vo;
                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String createSalesOrder(SalesOrderSaveRequest request) {
        SalesOrder order = new SalesOrder();
        order.setOrderId(codeGeneratorUtil.generateSalesOrderCode());
        order.setTenantCode(TenantPermissionContext.getTenantCode());
        order.setCreator(resolveCurrentUser());
        applySalesOrderContent(order, request, true);
        salesOrderMapper.insert(order);
        replaceSalesOrderItems(order.getOrderId(), request.getItems());
        insertSalesLog(order, null, "create", "创建销售订单");

        if (Objects.equals(request.getCreateProductionOrder(), 1)) {
            createProductionOrdersFromSales(order, request.getItems());
        }
        return order.getOrderId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSalesOrder(String orderId, SalesOrderSaveRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
        applySalesOrderContent(order, request, false);
        salesOrderMapper.updateById(order);
        replaceSalesOrderItems(orderId, request.getItems());
        if (!Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark())) {
            insertSalesLog(order, oldStatus, "status_change", blankToNull(request.getRemark()));
        }
        syncLinkedProductionOrders(order, oldStatus);
    }

    /**
     * 旧的轻量状态维护接口继续保留，避免已有调用链路失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSalesOrder(String orderId, SalesOrderUpdateRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
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
        if (request.getIsInvoice() != null) {
            order.setIsInvoice(normalizeInvoiceFlag(request.getIsInvoice()));
        }
        if (request.getRemark() != null) {
            order.setRemark(blankToNull(request.getRemark()));
        }
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        salesOrderMapper.updateById(order);
        if (!Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark())) {
            insertSalesLog(order, oldStatus, "status_change", blankToNull(request.getRemark()));
        }
        syncLinkedProductionOrders(order, oldStatus);
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
    public String createProductionOrder(ProductionOrderSaveRequest request) {
        ProductionOrder order = new ProductionOrder();
        order.setOrderId(codeGeneratorUtil.generateProductionOrderCode());
        order.setTenantCode(TenantPermissionContext.getTenantCode());
        order.setCreator(resolveCurrentUser());
        order.setUpdater(resolveCurrentUser());
        applyProductionOrderContent(order, request);
        productionOrderMapper.insert(order);
        return order.getOrderId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveProductionOrder(String orderId, ProductionOrderSaveRequest request) {
        ProductionOrder order = findProductionOrder(orderId);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        String oldStatus = order.getStatus();
        Integer oldProcess = order.getProcess();

        order.setUpdater(resolveCurrentUser());
        applyProductionOrderContent(order, request);
        productionOrderMapper.updateById(order);

        if (!Objects.equals(oldStatus, order.getStatus())
                || !Objects.equals(oldProcess, order.getProcess())
                || StringUtils.hasText(request.getRemark())) {
            insertProductionLog(order, oldStatusText, blankToNull(request.getRemark()));
        }
        syncLinkedSalesOrder(order, oldStatus);
    }

    /**
     * 旧的轻量状态维护接口继续保留，避免已有调用链路失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductionOrder(String orderId, ProductionOrderUpdateRequest request) {
        ProductionOrder order = findProductionOrder(orderId);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        String oldStatus = order.getStatus();
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
            insertProductionLog(order, oldStatusText, StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "管理端更新订单状态");
        }
        syncLinkedSalesOrder(order, oldStatus);
    }

    public Map<String, Object> checkOrderTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("salesOrderTableReady", salesOrderMapper.selectCount(null) >= 0);
        result.put("salesOrderDetailTableReady", salesOrderDetailMapper.selectCount(null) >= 0);
        result.put("salesOrderLogTableReady", salesOrderStatusLogMapper.selectCount(null) >= 0);
        result.put("productionOrderTableReady", productionOrderMapper.selectCount(null) >= 0);
        result.put("productionOrderLogTableReady", productionOrderStatusLogMapper.selectCount(null) >= 0);
        result.put("checkedAt", LocalDateTime.now());
        return result;
    }

    private void applySalesOrderContent(SalesOrder order, SalesOrderSaveRequest request, boolean createMode) {
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerPhone(blankToNull(request.getCustomerPhone()));
        order.setProjectName(request.getProjectName().trim());
        order.setDeliveryDate(requireText(request.getDeliveryDate(), "销售订单交付日期不能为空"));
        order.setExpressCompany(blankToNull(request.getExpressCompany()));
        order.setExpressNo(blankToNull(request.getExpressNo()));
        order.setIsInvoice(normalizeInvoiceFlag(request.getIsInvoice()));
        order.setRemark(blankToNull(request.getRemark()));
        order.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : defaultSalesStatus(order.getStatus()));
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        ensureCustomerProjectExists(order.getCustomerName(), order.getProjectName());
        order.setGoodsDesc(buildSalesGoodsDesc(request.getItems()));
        order.setTotalQuantity(sumSalesQuantity(request.getItems()));
        if (createMode && order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
        }
    }

    private Integer normalizeInvoiceFlag(Integer value) {
        return Objects.equals(value, 1) ? 1 : 0;
    }

    private void replaceSalesOrderItems(String orderId, List<SalesOrderSaveRequest.ItemDTO> items) {
        salesOrderDetailMapper.delete(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId));

        List<SalesOrderDetail> details = items.stream().map(item -> {
            SalesOrderDetail detail = new SalesOrderDetail();
            detail.setOrderId(orderId);
            detail.setTenantCode(TenantPermissionContext.getTenantCode());
            detail.setModelCode(item.getModelCode().trim());
            detail.setWeight(item.getWeight());
            detail.setSpec(formatNumber(item.getSpec()));
            detail.setQuantity(item.getQuantity());
            return detail;
        }).toList();

        details.forEach(salesOrderDetailMapper::insert);
    }

    private void createProductionOrdersFromSales(SalesOrder order, List<SalesOrderSaveRequest.ItemDTO> items) {
        items.forEach(item -> {
            ProductionOrder productionOrder = new ProductionOrder();
            productionOrder.setTenantCode(TenantPermissionContext.getTenantCode());
            productionOrder.setOrderId(codeGeneratorUtil.generateProductionOrderCode());
            productionOrder.setSalesOrderId(order.getOrderId());
            productionOrder.setStatus(DEFAULT_PRODUCTION_STATUS);
            productionOrder.setModelCode(item.getModelCode().trim());
            productionOrder.setWeight(toBigDecimal(item.getWeight()));
            productionOrder.setWidth(toBigDecimal(item.getSpec()));
            productionOrder.setQuantity(item.getQuantity() == null ? 0 : item.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue());
            productionOrder.setCustomerName(order.getCustomerName());
            productionOrder.setProjectName(order.getProjectName());
            productionOrder.setDeliveryDate(parseDateTime(order.getDeliveryDate()));
            productionOrder.setCreator(resolveCurrentUser());
            productionOrder.setUpdater(resolveCurrentUser());
            productionOrderMapper.insert(productionOrder);
        });
    }

    private void applyProductionOrderContent(ProductionOrder order, ProductionOrderSaveRequest request) {
        order.setSalesOrderId(blankToNull(request.getSalesOrderId()));
        order.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : defaultProductionStatus(order.getStatus()));
        order.setModelCode(request.getModelCode().trim());
        order.setFabric(blankToNull(request.getFabric()));
        order.setWeight(toBigDecimal(request.getWeight()));
        order.setWidth(toBigDecimal(request.getSpec()));
        order.setColor(blankToNull(request.getColor()));
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setTotalAmount(request.getPrice() == null ? null : request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setCustomerName(blankToNull(request.getCustomerName()));
        order.setProjectName(blankToNull(request.getProjectName()));
        order.setContactPhone(blankToNull(request.getContactPhone()));
        order.setDeliveryDate(parseDateTime(request.getDeliveryDate()));
        order.setProcess(resolveProcess(order.getStatus(), request.getProcess()));
    }

    private void insertProductionLog(ProductionOrder order, String oldStatusText, String remark) {
        ProductionOrderStatusLog log = new ProductionOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatusText);
        log.setNewStatus(buildProductionStateText(order.getStatus(), order.getProcess()));
        log.setOperateType(order.getProcess() != null ? "process_change" : "status_change");
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端更新订单信息");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(LocalDateTime.now());
        productionOrderStatusLogMapper.insert(log);
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String operateType, String remark) {
        SalesOrderStatusLog log = new SalesOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(order.getStatus());
        log.setOperateType(operateType);
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端更新销售订单信息");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(LocalDateTime.now());
        salesOrderStatusLogMapper.insert(log);
    }

    /**
     * 订单页允许先录业务，再自动把客户基础档案补入客户管理，减少人工来回切页维护。
     */
    private void ensureCustomerProjectExists(String customerName, String projectName) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getCustomerName, customerName)
                .last("LIMIT 1"));
        if (customer == null) {
            customer = new Customer();
            customer.setTenantCode(tenantCode);
            customer.setCustomerName(customerName);
            customer.setCustomerType(1);
            customerMapper.insert(customer);
        }

        Long projectCount = customerProjectMapper.selectCount(new LambdaQueryWrapper<CustomerProject>()
                .eq(CustomerProject::getTenantCode, tenantCode)
                .eq(CustomerProject::getCustomerId, customer.getId())
                .eq(CustomerProject::getProjectName, projectName));
        if (projectCount == null || projectCount == 0) {
            CustomerProject project = new CustomerProject();
            project.setTenantCode(tenantCode);
            project.setCustomerId(customer.getId());
            project.setProjectName(projectName);
            customerProjectMapper.insert(project);
        }
    }

    /**
     * 销售订单发货由物流驱动，所以只有变更为已发货时才强制校验物流信息。
     */
    private void validateShippingInfo(String status, String expressCompany, String expressNo) {
        if (!STATUS_SHIPPED.equals(status)) {
            return;
        }
        if (!StringUtils.hasText(expressCompany) || !StringUtils.hasText(expressNo)) {
            throw new BusinessException("订单变更为已发货时必须填写物流公司和物流单号");
        }
    }

    /**
     * 销售订单是交付侧主单，状态变化后优先把关联生产单同步到同一阶段。
     */
    private void syncLinkedProductionOrders(SalesOrder salesOrder, String oldStatus) {
        if (Objects.equals(oldStatus, salesOrder.getStatus()) || !supportsProductionStatusSync(salesOrder.getStatus())) {
            return;
        }
        List<ProductionOrder> linkedOrders = productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, salesOrder.getTenantCode())
                .eq(ProductionOrder::getSalesOrderId, salesOrder.getOrderId()));
        for (ProductionOrder linkedOrder : linkedOrders) {
            if (Objects.equals(linkedOrder.getStatus(), salesOrder.getStatus())) {
                continue;
            }
            String oldStatusText = buildProductionStateText(linkedOrder.getStatus(), linkedOrder.getProcess());
            linkedOrder.setStatus(salesOrder.getStatus());
            if (!STATUS_PENDING_CONFIRM.equals(salesOrder.getStatus()) && !STATUS_PENDING_SHIP.equals(salesOrder.getStatus())) {
                linkedOrder.setProcess(null);
            }
            productionOrderMapper.updateById(linkedOrder);
            insertProductionLog(linkedOrder, oldStatusText, "销售订单状态同步更新");
        }
    }

    /**
     * 生产订单只回写销售订单的共享状态，避免把备料中、生产中这类内部状态写回销售侧。
     * 当生产单回写已发货时，销售单必须已经具备完整物流信息。
     */
    private void syncLinkedSalesOrder(ProductionOrder productionOrder, String oldStatus) {
        if (Objects.equals(oldStatus, productionOrder.getStatus()) || !supportsSalesStatusSync(productionOrder.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(productionOrder.getSalesOrderId())) {
            return;
        }
        SalesOrder linkedSalesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, productionOrder.getTenantCode())
                .eq(SalesOrder::getOrderId, productionOrder.getSalesOrderId())
                .last("LIMIT 1"));
        if (linkedSalesOrder == null || Objects.equals(linkedSalesOrder.getStatus(), productionOrder.getStatus())) {
            return;
        }
        String oldSalesStatus = linkedSalesOrder.getStatus();
        linkedSalesOrder.setStatus(productionOrder.getStatus());
        validateShippingInfo(linkedSalesOrder.getStatus(), linkedSalesOrder.getExpressCompany(), linkedSalesOrder.getExpressNo());
        salesOrderMapper.updateById(linkedSalesOrder);
        insertSalesLog(linkedSalesOrder, oldSalesStatus, "sync", "生产订单状态同步更新");
    }

    private boolean supportsProductionStatusSync(String status) {
        return STATUS_PENDING_CONFIRM.equals(status)
                || STATUS_PENDING_SHIP.equals(status)
                || STATUS_SHIPPED.equals(status)
                || STATUS_COMPLETED.equals(status);
    }

    private boolean supportsSalesStatusSync(String status) {
        return STATUS_PENDING_CONFIRM.equals(status)
                || STATUS_PENDING_SHIP.equals(status)
                || STATUS_SHIPPED.equals(status)
                || STATUS_COMPLETED.equals(status);
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

    private String buildSalesGoodsDesc(List<SalesOrderSaveRequest.ItemDTO> items) {
        return items.stream()
                .map(item -> item.getModelCode().trim() + " / " + formatNumber(item.getWeight()) + "克 / " + formatNumber(item.getSpec()) + "规格 × " + item.getQuantity().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("；"));
    }

    private Integer sumSalesQuantity(List<SalesOrderSaveRequest.ItemDTO> items) {
        BigDecimal total = items.stream()
                .map(SalesOrderSaveRequest.ItemDTO::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.intValue();
    }

    private Integer resolveProcess(String status, Integer process) {
        if (!StringUtils.hasText(status) || !"producing".equals(status)) {
            return null;
        }
        return process;
    }

    private String defaultSalesStatus(String currentStatus) {
        return StringUtils.hasText(currentStatus) ? currentStatus : DEFAULT_SALES_STATUS;
    }

    private String defaultProductionStatus(String currentStatus) {
        return StringUtils.hasText(currentStatus) ? currentStatus : DEFAULT_PRODUCTION_STATUS;
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

    private String resolveCurrentUser() {
        Long userId = TenantPermissionContext.getUserId();
        return userId == null ? "system" : String.valueOf(userId);
    }

    private String resolveCurrentUserName() {
        Long userId = TenantPermissionContext.getUserId();
        if (userId == null) {
            return "系统";
        }
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Employee::getId, userId)
                .last("LIMIT 1"));
        if (employee != null && StringUtils.hasText(employee.getName())) {
            return employee.getName();
        }
        return String.valueOf(userId);
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

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
    }

    private BigDecimal toBigDecimal(Float value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }

    private String formatNumber(Float value) {
        if (value == null) {
            return "";
        }
        return BigDecimal.valueOf(value.doubleValue()).stripTrailingZeros().toPlainString();
    }
}
