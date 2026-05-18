package my.management.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.security.InternalUploadUrlValidator;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.customer.mapper.CustomerContactMapper;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.entity.Customer;
import my.management.module.customer.model.entity.CustomerContact;
import my.management.module.customer.model.entity.CustomerProject;
import my.management.module.customer.model.enums.CustomerTypeEnum;
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
import my.management.module.order.model.dto.OrderWarningSettingUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.ProductionOrderStatusLog;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.entity.SalesOrderDetail;
import my.management.module.order.model.entity.SalesOrderStatusLog;
import my.management.module.order.model.enums.OrderLogOperateTypeEnum;
import my.management.module.order.model.enums.OrderStatusEnum;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.OrderWarningSettingVO;
import my.management.module.order.model.vo.OrderWarningSummaryVO;
import my.management.module.order.model.vo.SalesOrderAttachmentVO;
import my.management.module.order.model.vo.SalesOrderDetailVO;
import my.management.module.order.model.vo.SalesOrderPageVO;
import my.management.module.order.model.vo.SalesOrderStatusLogVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单管理服务，统一承接销售订单与生产订单的查询、创建和完整维护。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ATTACHMENT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 200L;
    private static final List<String> SALES_STATUS_CODES = List.of(
            "pending_confirm", "pending_pay", "pending_material", "producing", "pending_ship", "shipped", "completed", "cancelled"
    );
    private static final List<String> PRODUCTION_STATUS_CODES = List.of(
            "pending_confirm", "pending_material", "producing", "pending_ship", "shipped", "completed"
    );
    private static final String STATUS_PENDING_PAY = OrderStatusEnum.PENDING_PAY.getCode();
    private static final String STATUS_PRODUCING = OrderStatusEnum.PRODUCING.getCode();
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "doc", "docx", "xls", "xlsx", "txt", "zip", "rar"
    );

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
    private CustomerContactMapper customerContactMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private OrderSettingService orderSettingService;

    @Resource
    private OrderWarningCacheService orderWarningCacheService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public SalesOrderAttachmentVO uploadSalesAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的订单附件");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException("订单附件不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename() == null ? "attachment" : StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new BusinessException("附件文件名不合法");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(normalizedExtension)) {
            throw new BusinessException("仅支持 PDF、图片、Word、Excel、文本或压缩包附件");
        }

        String tenantFolder = safePathSegment(TenantPermissionContext.getTenantCode());
        String dateFolder = LocalDate.now().format(ATTACHMENT_DATE_FORMATTER);
        Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path targetDir = rootPath.resolve("sales-order").resolve(tenantFolder).resolve(dateFolder).normalize();
        if (!targetDir.startsWith(rootPath)) {
            throw new BusinessException("附件存储路径不合法");
        }

        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + normalizedExtension;
        Path targetPath = targetDir.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new BusinessException("附件存储路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException("订单附件上传失败，请稍后重试");
        }

        SalesOrderAttachmentVO vo = new SalesOrderAttachmentVO();
        vo.setFileName(originalFilename);
        vo.setFileSize(file.getSize());
        vo.setFileUrl(resolveContextPath() + "/uploads/sales-order/" + tenantFolder + "/" + dateFolder + "/" + storedFilename);
        return vo;
    }

    public org.springframework.core.io.Resource loadSalesAttachment(String attachmentUrl) {
        String relativePath = normalizeAttachmentPath(attachmentUrl);
        String currentTenantFolder = safePathSegment(TenantPermissionContext.getTenantCode());
        String currentTenantPrefix = "sales-order/" + currentTenantFolder + "/";
        if (!relativePath.startsWith(currentTenantPrefix)) {
            throw new BusinessException("订单附件不存在或无权访问");
        }

        Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path salesOrderRoot = rootPath.resolve("sales-order").normalize();
        Path targetPath = rootPath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(salesOrderRoot) || !Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new BusinessException("订单附件不存在或已被移除");
        }
        return new FileSystemResource(targetPath);
    }

    public Page<SalesOrderPageVO> pageSalesOrders(SalesOrderPageRequest request) {
        if (request == null) {
            request = new SalesOrderPageRequest();
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        int staleWarningDays = orderSettingService.staleWarningDays(tenantCode);
        Page<SalesOrder> page = new Page<>(safePage(request.getPageNum()), safeSize(request.getPageSize()));

        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .orderByDesc(SalesOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(SalesOrder::getStatus, request.getStatus().trim());
        }
        if (StringUtils.hasText(request.getCustomerName())) {
            wrapper.like(SalesOrder::getCustomerName, request.getCustomerName().trim());
        }
        if (request.getIsInvoice() != null) {
            wrapper.eq(SalesOrder::getIsInvoice, normalizeInvoiceFlag(request.getIsInvoice()));
        }
        if (request.getDeliveryStart() != null) {
            wrapper.ge(SalesOrder::getDeliveryDate, request.getDeliveryStart().toString());
        }
        if (request.getDeliveryEnd() != null) {
            wrapper.le(SalesOrder::getDeliveryDate, request.getDeliveryEnd().toString());
        }
        applyCreateTimeRange(wrapper, request.getCreateStart(), request.getCreateEnd(), SalesOrder::getCreateTime);
        if (Boolean.TRUE.equals(request.getStaleOnly())) {
            applySalesStaleWarningFilter(wrapper, staleWarningDays);
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
            markSalesStaleWarning(vo, order, staleWarningDays);
            return vo;
        }).toList();

        Page<SalesOrderPageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    public Map<String, Long> countSalesOrderStatuses() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode))));
        for (String status : SALES_STATUS_CODES) {
            result.put(status, safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                    .eq(SalesOrder::getTenantCode, tenantCode)
                    .eq(SalesOrder::getStatus, status))));
        }
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
        LocalDateTime businessCreateTime = resolveBusinessCreateTime(request.getCreateTime(), "销售订单录单时间");
        order.setCreateTime(businessCreateTime);
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.insert(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        replaceSalesOrderItems(order.getOrderId(), request.getItems(), businessCreateTime);
        insertSalesLog(order, null, OrderLogOperateTypeEnum.CREATE.getCode(), "创建销售订单", businessCreateTime);

        if (BinaryFlagEnum.isYes(request.getCreateProductionOrder())) {
            createProductionOrdersFromSales(order, request.getItems());
        }
        return order.getOrderId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSalesOrder(String orderId, SalesOrderSaveRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
        applySalesOrderContent(order, request, false);
        assertDirectSalesTransitionAllowed(oldStatus, order.getStatus());
        applyManualCreateTime(order, request.getCreateTime(), "销售订单录单时间");
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        replaceSalesOrderItems(orderId, request.getItems(), order.getCreateTime());
        if (!Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark())) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(), blankToNull(request.getRemark()));
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
        assertDirectSalesTransitionAllowed(oldStatus, order.getStatus());
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
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        if (!Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark())) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(), blankToNull(request.getRemark()));
        }
        syncLinkedProductionOrders(order, oldStatus);
    }

    /**
     * 订单审批中心专用入口：只有审批通过后才允许待收款销售订单进入生产中。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveSalesOrderTransition(String orderId, String targetStatus, String remark) {
        if (!StringUtils.hasText(targetStatus)) {
            throw new BusinessException("目标订单状态不能为空");
        }
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
        order.setStatus(targetStatus.trim());
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        if (!Objects.equals(oldStatus, order.getStatus())) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(),
                    StringUtils.hasText(remark) ? remark.trim() : "审批中心确认销售订单流转");
        }
        syncLinkedProductionOrders(order, oldStatus);
    }

    public Page<ProductionOrderPageVO> pageProductionOrders(ProductionOrderPageRequest request) {
        if (request == null) {
            request = new ProductionOrderPageRequest();
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        int staleWarningDays = orderSettingService.staleWarningDays(tenantCode);
        Page<ProductionOrder> page = new Page<>(safePage(request.getPageNum()), safeSize(request.getPageSize()));

        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, tenantCode)
                .orderByDesc(ProductionOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ProductionOrder::getStatus, request.getStatus().trim());
        }
        if (StringUtils.hasText(request.getCustomerName())) {
            wrapper.like(ProductionOrder::getCustomerName, request.getCustomerName().trim());
        }
        if (request.getProcess() != null) {
            wrapper.eq(ProductionOrder::getProcess, request.getProcess());
        }
        if (request.getDeliveryStart() != null) {
            wrapper.ge(ProductionOrder::getDeliveryDate, request.getDeliveryStart().atStartOfDay());
        }
        if (request.getDeliveryEnd() != null) {
            wrapper.lt(ProductionOrder::getDeliveryDate, request.getDeliveryEnd().plusDays(1).atStartOfDay());
        }
        applyCreateTimeRange(wrapper, request.getCreateStart(), request.getCreateEnd(), ProductionOrder::getCreateTime);
        if (Boolean.TRUE.equals(request.getStaleOnly())) {
            applyProductionStaleWarningFilter(wrapper, staleWarningDays);
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
            markProductionStaleWarning(vo, order, staleWarningDays);
            return vo;
        }).toList();

        Page<ProductionOrderPageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    public Map<String, Long> countProductionOrderStatuses() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", safeCount(productionOrderMapper.selectCount(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, tenantCode))));
        for (String status : PRODUCTION_STATUS_CODES) {
            result.put(status, safeCount(productionOrderMapper.selectCount(new LambdaQueryWrapper<ProductionOrder>()
                    .eq(ProductionOrder::getTenantCode, tenantCode)
                    .eq(ProductionOrder::getStatus, status))));
        }
        return result;
    }

    public OrderWarningSettingVO getOrderWarningSetting() {
        return orderSettingService.currentSetting();
    }

    public OrderWarningSettingVO updateOrderWarningSetting(OrderWarningSettingUpdateRequest request) {
        return orderSettingService.updateCurrentSetting(request);
    }

    public OrderWarningSummaryVO getOrderWarningSummary() {
        return orderWarningCacheService.summary(TenantPermissionContext.getTenantCode());
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
        LocalDateTime businessCreateTime = resolveBusinessCreateTime(request.getCreateTime(), "生产订单录单时间");
        order.setCreateTime(businessCreateTime);
        order.setUpdateTime(LocalDateTime.now());
        productionOrderMapper.insert(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
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
        applyManualCreateTime(order, request.getCreateTime(), "生产订单录单时间");
        order.setUpdateTime(LocalDateTime.now());
        productionOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());

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

        order.setUpdateTime(LocalDateTime.now());
        productionOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());

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
        order.setAttachmentName(blankToNull(request.getAttachmentName()));
        order.setAttachmentUrl(normalizeSalesOrderAttachmentUrlForStorage(request.getAttachmentUrl()));
        order.setAttachmentSize(request.getAttachmentSize());
        order.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : defaultSalesStatus(order.getStatus()));
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        ensureCustomerProjectExists(order.getCustomerName(), order.getCustomerPhone(), order.getProjectName());
        order.setGoodsDesc(buildSalesGoodsDesc(request.getItems()));
        order.setTotalQuantity(sumSalesQuantity(request.getItems()));
        if (createMode && order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
        }
    }

    private Integer normalizeInvoiceFlag(Integer value) {
        return BinaryFlagEnum.isYes(value) ? BinaryFlagEnum.YES.getCode() : BinaryFlagEnum.NO.getCode();
    }

    private void replaceSalesOrderItems(String orderId, List<SalesOrderSaveRequest.ItemDTO> items, LocalDateTime businessCreateTime) {
        salesOrderDetailMapper.delete(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId));

        LocalDateTime now = LocalDateTime.now();
        List<SalesOrderDetail> details = items.stream().map(item -> {
            SalesOrderDetail detail = new SalesOrderDetail();
            detail.setOrderId(orderId);
            detail.setTenantCode(TenantPermissionContext.getTenantCode());
            detail.setModelCode(item.getModelCode().trim());
            detail.setWeight(item.getWeight());
            detail.setSpec(formatNumber(item.getSpec()));
            detail.setQuantity(item.getQuantity());
            detail.setCreateTime(businessCreateTime == null ? now : businessCreateTime);
            detail.setUpdateTime(now);
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
            productionOrder.setStatus(OrderStatusEnum.PENDING_CONFIRM.getCode());
            productionOrder.setModelCode(item.getModelCode().trim());
            productionOrder.setWeight(toBigDecimal(item.getWeight()));
            productionOrder.setWidth(toBigDecimal(item.getSpec()));
            productionOrder.setQuantity(item.getQuantity() == null ? 0 : item.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue());
            productionOrder.setCustomerName(order.getCustomerName());
            productionOrder.setProjectName(order.getProjectName());
            productionOrder.setDeliveryDate(parseDateTime(order.getDeliveryDate()));
            productionOrder.setCreator(resolveCurrentUser());
            productionOrder.setUpdater(resolveCurrentUser());
            productionOrder.setCreateTime(order.getCreateTime());
            productionOrder.setUpdateTime(LocalDateTime.now());
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
        ensureCustomerProjectExists(order.getCustomerName(), order.getContactPhone(), order.getProjectName());
    }

    private void insertProductionLog(ProductionOrder order, String oldStatusText, String remark) {
        ProductionOrderStatusLog log = new ProductionOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatusText);
        log.setNewStatus(buildProductionStateText(order.getStatus(), order.getProcess()));
        log.setOperateType(order.getProcess() != null
                ? OrderLogOperateTypeEnum.PROCESS_CHANGE.getCode()
                : OrderLogOperateTypeEnum.STATUS_CHANGE.getCode());
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端更新订单信息");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(LocalDateTime.now());
        productionOrderStatusLogMapper.insert(log);
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String operateType, String remark) {
        insertSalesLog(order, oldStatus, operateType, remark, LocalDateTime.now());
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String operateType, String remark, LocalDateTime createTime) {
        SalesOrderStatusLog log = new SalesOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(order.getStatus());
        log.setOperateType(operateType);
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端更新销售订单信息");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(createTime == null ? LocalDateTime.now() : createTime);
        salesOrderStatusLogMapper.insert(log);
    }

    /**
     * 订单页允许先录业务，再自动把客户基础档案补入客户管理，减少人工来回切页维护。
     */
    private void ensureCustomerProjectExists(String customerName, String customerPhone, String projectName) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String normalizedCustomerName = blankToNull(customerName);
        if (!StringUtils.hasText(normalizedCustomerName)) {
            return;
        }
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getCustomerName, normalizedCustomerName)
                .last("LIMIT 1"));
        if (customer == null) {
            customer = new Customer();
            customer.setTenantCode(tenantCode);
            customer.setCustomerName(normalizedCustomerName);
            customer.setCustomerType(CustomerTypeEnum.DEFAULT.getCode());
            customerMapper.insert(customer);
        }

        ensureCustomerContactExists(tenantCode, customer.getId(), normalizedCustomerName, customerPhone);

        String normalizedProjectName = blankToNull(projectName);
        if (!StringUtils.hasText(normalizedProjectName)) {
            return;
        }
        Long projectCount = customerProjectMapper.selectCount(new LambdaQueryWrapper<CustomerProject>()
                .eq(CustomerProject::getTenantCode, tenantCode)
                .eq(CustomerProject::getCustomerId, customer.getId())
                .eq(CustomerProject::getProjectName, normalizedProjectName));
        if (projectCount == null || projectCount == 0) {
            CustomerProject project = new CustomerProject();
            project.setTenantCode(tenantCode);
            project.setCustomerId(customer.getId());
            project.setProjectName(normalizedProjectName);
            customerProjectMapper.insert(project);
        }
    }

    private void ensureCustomerContactExists(String tenantCode, Long customerId, String customerName, String customerPhone) {
        String normalizedPhone = blankToNull(customerPhone);
        if (!StringUtils.hasText(normalizedPhone)) {
            return;
        }
        Long contactCount = customerContactMapper.selectCount(new LambdaQueryWrapper<CustomerContact>()
                .eq(CustomerContact::getTenantCode, tenantCode)
                .eq(CustomerContact::getCustomerId, customerId)
                .eq(CustomerContact::getContactPhone, normalizedPhone));
        if (contactCount != null && contactCount > 0) {
            return;
        }
        CustomerContact contact = new CustomerContact();
        contact.setTenantCode(tenantCode);
        contact.setCustomerId(customerId);
        contact.setContactName(customerName);
        contact.setContactPhone(normalizedPhone);
        customerContactMapper.insert(contact);
    }

    /**
     * 销售订单发货由物流驱动，所以只有变更为已发货时才强制校验物流信息。
     */
    private void validateShippingInfo(String status, String expressCompany, String expressNo) {
        if (!OrderStatusEnum.SHIPPED.matches(status)) {
            return;
        }
        if (!StringUtils.hasText(expressCompany) || !StringUtils.hasText(expressNo)) {
            throw new BusinessException("订单变更为已发货时必须填写物流公司和物流单号");
        }
    }

    private void assertDirectSalesTransitionAllowed(String oldStatus, String targetStatus) {
        if (STATUS_PENDING_PAY.equals(oldStatus) && STATUS_PRODUCING.equals(targetStatus)) {
            throw new BusinessException("待收款订单转生产中需要先通过订单审批");
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
                .eq(ProductionOrder::getSalesOrderId, salesOrder.getOrderId()));
        for (ProductionOrder linkedOrder : linkedOrders) {
            if (Objects.equals(linkedOrder.getStatus(), salesOrder.getStatus())) {
                continue;
            }
            String oldStatusText = buildProductionStateText(linkedOrder.getStatus(), linkedOrder.getProcess());
            linkedOrder.setStatus(salesOrder.getStatus());
            if (!OrderStatusEnum.PENDING_CONFIRM.matches(salesOrder.getStatus()) && !OrderStatusEnum.PENDING_SHIP.matches(salesOrder.getStatus())) {
                linkedOrder.setProcess(null);
            }
            linkedOrder.setUpdateTime(LocalDateTime.now());
            productionOrderMapper.updateById(linkedOrder);
            orderWarningCacheService.invalidate(linkedOrder.getTenantCode());
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
                .eq(SalesOrder::getOrderId, productionOrder.getSalesOrderId())
                .last("LIMIT 1"));
        if (linkedSalesOrder == null || Objects.equals(linkedSalesOrder.getStatus(), productionOrder.getStatus())) {
            return;
        }
        String oldSalesStatus = linkedSalesOrder.getStatus();
        linkedSalesOrder.setStatus(productionOrder.getStatus());
        validateShippingInfo(linkedSalesOrder.getStatus(), linkedSalesOrder.getExpressCompany(), linkedSalesOrder.getExpressNo());
        linkedSalesOrder.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(linkedSalesOrder);
        orderWarningCacheService.invalidate(linkedSalesOrder.getTenantCode());
        insertSalesLog(linkedSalesOrder, oldSalesStatus, OrderLogOperateTypeEnum.SYNC.getCode(), "生产订单状态同步更新");
    }

    private boolean supportsProductionStatusSync(String status) {
        return OrderStatusEnum.supportsSalesProductionSync(status);
    }

    private boolean supportsSalesStatusSync(String status) {
        return OrderStatusEnum.supportsSalesProductionSync(status);
    }

    private SalesOrder findSalesOrder(String orderId) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("sales order not found");
        }
        return order;
    }

    private ProductionOrder findProductionOrder(String orderId) {
        ProductionOrder order = productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
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
        if (!StringUtils.hasText(status) || !OrderStatusEnum.PRODUCING.matches(status)) {
            return null;
        }
        return process;
    }

    private String defaultSalesStatus(String currentStatus) {
        return OrderStatusEnum.defaultIfBlank(currentStatus, OrderStatusEnum.PENDING_CONFIRM);
    }

    private String defaultProductionStatus(String currentStatus) {
        return OrderStatusEnum.defaultIfBlank(currentStatus, OrderStatusEnum.PENDING_CONFIRM);
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
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safePathSegment(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "public";
        return text.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String normalizeSalesOrderAttachmentUrlForStorage(String attachmentUrl) {
        return InternalUploadUrlValidator.normalizeStoredUploadUrl(
                attachmentUrl,
                resolveContextPath(),
                TenantPermissionContext.getTenantCode(),
                "sales-order"
        );
    }

    private String normalizeAttachmentPath(String attachmentUrl) {
        String path = InternalUploadUrlValidator.normalizeRelativeUploadPath(
                attachmentUrl,
                resolveContextPath(),
                TenantPermissionContext.getTenantCode(),
                "sales-order"
        );
        if (!StringUtils.hasText(path)) {
            throw new BusinessException("附件地址不能为空");
        }
        return path;
    }

    private String resolveContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private <T> void applyCreateTimeRange(LambdaQueryWrapper<T> wrapper,
                                          LocalDate start,
                                          LocalDate end,
                                          SFunction<T, LocalDateTime> column) {
        if (start != null) {
            wrapper.ge(column, start.atStartOfDay());
        }
        if (end != null) {
            wrapper.lt(column, end.plusDays(1).atStartOfDay());
        }
    }

    private void applySalesStaleWarningFilter(LambdaQueryWrapper<SalesOrder> wrapper, int staleWarningDays) {
        wrapper.notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(), OrderStatusEnum.CANCELLED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(staleWarningDays));
    }

    private void applyProductionStaleWarningFilter(LambdaQueryWrapper<ProductionOrder> wrapper, int staleWarningDays) {
        wrapper.ne(ProductionOrder::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .apply("COALESCE(update_time, create_time) <= {0}", LocalDateTime.now().minusDays(staleWarningDays));
    }

    private void markSalesStaleWarning(SalesOrderPageVO vo, SalesOrder order, int staleWarningDays) {
        long days = staleDays(order.getUpdateTime(), order.getCreateTime());
        vo.setStaleWarningDays(staleWarningDays);
        vo.setStaleDays(days);
        vo.setStaleWarning(salesOrderStaleCandidate(order.getStatus())
                && isStale(order.getUpdateTime(), order.getCreateTime(), staleWarningDays));
    }

    private void markProductionStaleWarning(ProductionOrderPageVO vo, ProductionOrder order, int staleWarningDays) {
        long days = staleDays(order.getUpdateTime(), order.getCreateTime());
        vo.setStaleWarningDays(staleWarningDays);
        vo.setStaleDays(days);
        vo.setStaleWarning(productionOrderStaleCandidate(order.getStatus())
                && isStale(order.getUpdateTime(), order.getCreateTime(), staleWarningDays));
    }

    private boolean salesOrderStaleCandidate(String status) {
        return !OrderStatusEnum.COMPLETED.matches(status) && !OrderStatusEnum.CANCELLED.matches(status);
    }

    private boolean productionOrderStaleCandidate(String status) {
        return !OrderStatusEnum.COMPLETED.matches(status);
    }

    private boolean isStale(LocalDateTime updateTime, LocalDateTime createTime, int staleWarningDays) {
        LocalDateTime anchor = updateTime != null ? updateTime : createTime;
        return anchor != null && !anchor.isAfter(LocalDateTime.now().minusDays(staleWarningDays));
    }

    private long staleDays(LocalDateTime updateTime, LocalDateTime createTime) {
        LocalDateTime anchor = updateTime != null ? updateTime : createTime;
        if (anchor == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(anchor, LocalDateTime.now()));
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalizeDateTimeText(value);
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized).atStartOfDay();
        }
        return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
    }

    private LocalDateTime resolveBusinessCreateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return parseDateTime(value);
        } catch (RuntimeException ex) {
            throw new BusinessException(fieldName + "格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }

    private void applyManualCreateTime(SalesOrder order, String value, String fieldName) {
        if (StringUtils.hasText(value)) {
            order.setCreateTime(resolveBusinessCreateTime(value, fieldName));
        }
    }

    private void applyManualCreateTime(ProductionOrder order, String value, String fieldName) {
        if (StringUtils.hasText(value)) {
            order.setCreateTime(resolveBusinessCreateTime(value, fieldName));
        }
    }

    private String normalizeDateTimeText(String value) {
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        return normalized;
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
