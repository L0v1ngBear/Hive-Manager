package my.management.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.context.OperationLogSkipContext;
import my.hive.common.exception.BusinessException;
import my.hive.common.order.OrderFlowCodeUtil;
import my.hive.common.print.PrintTaskService;
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
import my.management.module.order.model.dto.OrderFlowPrintTaskRequest;
import my.management.module.order.model.dto.OrderWarningSettingUpdateRequest;
import my.management.module.order.model.dto.SalesOrderPageRequest;
import my.management.module.order.model.dto.SalesOrderSaveRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.ProductionOrderStatusLog;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.entity.SalesOrderDetail;
import my.management.module.order.model.entity.SalesOrderStatusLog;
import my.management.module.order.model.enums.OrderCategoryEnum;
import my.management.module.order.model.enums.OrderLogOperateTypeEnum;
import my.management.module.order.model.enums.OrderStatusEnum;
import my.management.module.order.model.vo.ProductionOrderDetailVO;
import my.management.module.order.model.vo.ProductionOrderPageVO;
import my.management.module.order.model.vo.ProductionProcessStepVO;
import my.management.module.order.model.vo.ProductionOrderStatusLogVO;
import my.management.module.order.model.vo.OrderFlowPrintTaskVO;
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
            "budgeting", "budget_completed",
            "pending_confirm", "pending_pay", "pending_material", "producing", "pending_ship", "shipped", "completed", "cancelled"
    );
    private static final List<String> PRODUCTION_STATUS_CODES = List.of(
            "pending_confirm", "pending_material", "producing", "pending_ship", "shipped", "completed"
    );
    private static final List<String> PRODUCTION_PROCESS_LABELS = List.of(
            "原料入库", "原料检验", "尺寸裁剪", "窗帘缝制", "窗帘熨烫",
            "成品检验", "高温定型", "打包装箱", "成品入库", "成品发货"
    );
    private static final String STATUS_PENDING_PAY = OrderStatusEnum.PENDING_PAY.getCode();
    private static final String STATUS_PENDING_CONFIRM = OrderStatusEnum.PENDING_CONFIRM.getCode();
    private static final String STATUS_BUDGETING = OrderStatusEnum.BUDGETING.getCode();
    private static final String STATUS_BUDGET_COMPLETED = OrderStatusEnum.BUDGET_COMPLETED.getCode();
    private static final String STATUS_PRODUCING = OrderStatusEnum.PRODUCING.getCode();
    private static final String STATUS_PENDING_SHIP = OrderStatusEnum.PENDING_SHIP.getCode();
    private static final String CATEGORY_DRAWING_BUDGET = OrderCategoryEnum.DRAWING_BUDGET.getCode();
    private static final String CATEGORY_SAMPLE_ROOM = OrderCategoryEnum.SAMPLE_ROOM.getCode();
    private static final String CATEGORY_BULK = OrderCategoryEnum.BULK.getCode();
    private static final String CATEGORY_REPLENISHMENT = OrderCategoryEnum.REPLENISHMENT.getCode();
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

    @Resource
    private PrintTaskService printTaskService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${ORDER_FLOW_CODE_SECRET:${AUTH_TOKEN_SECRET:hive-local-order-flow-secret}}")
    private String orderFlowCodeSecret;

    public OrderFlowPrintTaskVO createSalesOrderFlowPrintTask(OrderFlowPrintTaskRequest request) {
        String orderId = requireOrderId(request);
        SalesOrder order = findSalesOrder(orderId);
        Map<String, Object> payload = buildSalesOrderFlowPrintPayload(order);
        String taskNo = printTaskService.createTask(
                "order_flow",
                "sales_order",
                order.getOrderId(),
                order.getOrderId(),
                payload,
                null,
                null,
                "网页端创建销售订单流转码，请在小程序端蓝牙打印");
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException("订单流转码打印任务创建失败");
        }
        payload.put("printTaskNo", taskNo);
        return buildOrderFlowPrintTaskVO(taskNo, order.getOrderId(), "sales", payload);
    }

    public OrderFlowPrintTaskVO createProductionOrderFlowPrintTask(OrderFlowPrintTaskRequest request) {
        String orderId = requireOrderId(request);
        ProductionOrder order = findProductionOrder(orderId);
        Map<String, Object> payload = buildProductionOrderFlowPrintPayload(order);
        String taskNo = printTaskService.createTask(
                "order_flow",
                "production_order",
                order.getOrderId(),
                order.getSalesOrderId(),
                payload,
                null,
                null,
                "网页端创建生产订单流转码，请在小程序端蓝牙打印");
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException("订单流转码打印任务创建失败");
        }
        payload.put("printTaskNo", taskNo);
        return buildOrderFlowPrintTaskVO(taskNo, order.getOrderId(), "production", payload);
    }

    public void enqueueProductionOrderFlowPrintTaskAfterApproval(String orderId) {
        ProductionOrder order = findProductionOrder(orderId);
        enqueueProductionOrderFlowPrintTaskIfAbsent(order, "订单审批通过，自动生成生产订单流转码待打印任务");
    }

    private void enqueueSalesOrderFlowPrintTaskIfAbsent(SalesOrder order, String reason) {
        Map<String, Object> payload = buildSalesOrderFlowPrintPayload(order);
        String taskNo = printTaskService.createTaskIfAbsent(
                "order_flow",
                "sales_order",
                order.getOrderId(),
                order.getOrderId(),
                payload,
                null,
                null,
                reason);
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException("订单审批已通过，但流转码打印任务创建失败，请重试");
        }
    }

    private void enqueueProductionOrderFlowPrintTaskIfAbsent(ProductionOrder order, String reason) {
        Map<String, Object> payload = buildProductionOrderFlowPrintPayload(order);
        String taskNo = printTaskService.createTaskIfAbsent(
                "order_flow",
                "production_order",
                order.getOrderId(),
                order.getSalesOrderId(),
                payload,
                null,
                null,
                reason);
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException("订单审批已通过，但流转码打印任务创建失败，请重试");
        }
    }

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
        if (StringUtils.hasText(request.getBrandName())) {
            wrapper.like(SalesOrder::getBrandName, request.getBrandName().trim());
        }
        if (StringUtils.hasText(request.getOrderCategory())) {
            wrapper.eq(SalesOrder::getOrderCategory, OrderCategoryEnum.normalize(request.getOrderCategory()));
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
            applySalesStaleWarningFilter(wrapper, tenantCode);
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(SalesOrder::getOrderId, keyword)
                    .or().like(SalesOrder::getCustomerName, keyword)
                    .or().like(SalesOrder::getCustomerPhone, keyword)
                    .or().like(SalesOrder::getProjectName, keyword)
                    .or().like(SalesOrder::getBrandName, keyword)
                    .or().like(SalesOrder::getGoodsDesc, keyword));
        }

        Page<SalesOrder> source = salesOrderMapper.selectPage(page, wrapper);
        List<SalesOrder> orders = source.getRecords();
        Map<String, Integer> detailCountMap = buildSalesDetailCountMap(orders);

        List<SalesOrderPageVO> records = orders.stream().map(order -> {
            SalesOrderPageVO vo = new SalesOrderPageVO();
            BeanUtils.copyProperties(order, vo);
            vo.setDetailCount(detailCountMap.getOrDefault(order.getOrderId(), 0));
            markSalesStaleWarning(vo, order, orderSettingService.staleWarningDays(tenantCode, order.getOrderCategory()));
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
        long total = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)));
        result.put("total", total);
        for (String status : SALES_STATUS_CODES) {
            result.put(status, safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                    .eq(SalesOrder::getTenantCode, tenantCode)
                    .eq(SalesOrder::getStatus, status))));
        }
        long sampleRoomCount = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.SAMPLE_ROOM.getCode())));
        long replenishmentCount = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.REPLENISHMENT.getCode())));
        long drawingBudgetCount = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.DRAWING_BUDGET.getCode())));
        long bulkCount = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.BULK.getCode())));
        result.put("category_sample_room", sampleRoomCount);
        result.put("category_replenishment", replenishmentCount);
        result.put("category_drawing_budget", drawingBudgetCount);
        result.put("category_bulk", bulkCount);
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

        if (BinaryFlagEnum.isYes(request.getCreateProductionOrder()) && !isDrawingBudgetOrder(order.getOrderCategory())) {
            createProductionOrdersFromSales(order, request.getItems());
        }
        return order.getOrderId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSalesOrder(String orderId, SalesOrderSaveRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        SalesOrder beforeOrder = copySalesOrder(order);
        String oldStatus = order.getStatus();
        boolean itemsChanged = salesOrderItemsChanged(orderId, request.getItems());
        applySalesOrderContent(order, request, false);
        assertDirectSalesTransitionAllowed(oldStatus, order.getStatus());
        applyManualCreateTime(order, request.getCreateTime(), "销售订单录单时间");
        boolean businessChanged = salesOrderContentChanged(beforeOrder, order) || itemsChanged;
        order.setUpdateTime(businessChanged ? LocalDateTime.now() : beforeOrder.getUpdateTime());
        salesOrderMapper.updateById(order);
        if (businessChanged) {
            orderWarningCacheService.invalidate(order.getTenantCode());
            replaceSalesOrderItems(orderId, request.getItems(), order.getCreateTime());
        }
        boolean logChanged = !Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark());
        if (logChanged) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(), blankToNull(request.getRemark()));
        }
        syncLinkedProductionOrders(order, oldStatus);
        if (!businessChanged && !logChanged) {
            OperationLogSkipContext.skipCurrent();
        }
    }

    /**
     * 旧的轻量状态维护接口继续保留，避免已有调用链路失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSalesOrder(String orderId, SalesOrderUpdateRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
        if (StringUtils.hasText(request.getStatus())) {
            order.setStatus(resolveSalesStatusForCategory(order.getOrderCategory(), request.getStatus(), oldStatus));
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
        enqueueSalesOrderFlowPrintTaskIfAbsent(order, "订单审批通过，自动生成销售订单流转码待打印任务");
        enqueueLinkedProductionOrderFlowPrintTasksIfApproved(order);
    }

    public Page<ProductionOrderPageVO> pageProductionOrders(ProductionOrderPageRequest request) {
        if (request == null) {
            request = new ProductionOrderPageRequest();
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
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
        if (StringUtils.hasText(request.getBrandName())) {
            wrapper.like(ProductionOrder::getBrandName, request.getBrandName().trim());
        }
        if (StringUtils.hasText(request.getOrderCategory())) {
            wrapper.eq(ProductionOrder::getOrderCategory, OrderCategoryEnum.normalize(request.getOrderCategory()));
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
            applyProductionStaleWarningFilter(wrapper, tenantCode);
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(ProductionOrder::getOrderId, keyword)
                    .or().like(ProductionOrder::getSalesOrderId, keyword)
                    .or().like(ProductionOrder::getCustomerName, keyword)
                    .or().like(ProductionOrder::getProjectName, keyword)
                    .or().like(ProductionOrder::getBrandName, keyword)
                    .or().like(ProductionOrder::getModelCode, keyword));
        }

        Page<ProductionOrder> source = productionOrderMapper.selectPage(page, wrapper);
        List<ProductionOrderPageVO> records = source.getRecords().stream().map(order -> {
            ProductionOrderPageVO vo = new ProductionOrderPageVO();
            BeanUtils.copyProperties(order, vo);
            fillProductionProcessView(vo, order.getStatus(), order.getProcess());
            markProductionStaleWarning(vo, order, orderSettingService.staleWarningDays(tenantCode, order.getOrderCategory()));
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

    public OrderWarningSummaryVO refreshOrderWarningSummary() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        orderWarningCacheService.invalidate(tenantCode);
        return orderWarningCacheService.summary(tenantCode);
    }

    public ProductionOrderDetailVO getProductionOrderDetail(String orderId) {
        ProductionOrder order = findProductionOrder(orderId);
        List<ProductionOrderStatusLogVO> logs = listProductionLogs(orderId);

        ProductionOrderDetailVO vo = new ProductionOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        fillProductionProcessView(vo, order.getStatus(), order.getProcess());
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
        ProductionOrder beforeOrder = copyProductionOrder(order);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        String oldStatus = order.getStatus();
        Integer oldProcess = order.getProcess();

        applyProductionOrderContent(order, request);
        applyManualCreateTime(order, request.getCreateTime(), "生产订单录单时间");
        boolean businessChanged = productionOrderContentChanged(beforeOrder, order);
        if (businessChanged) {
            order.setUpdater(resolveCurrentUser());
            order.setUpdateTime(LocalDateTime.now());
        } else {
            order.setUpdater(beforeOrder.getUpdater());
            order.setUpdateTime(beforeOrder.getUpdateTime());
        }
        productionOrderMapper.updateById(order);
        if (businessChanged) {
            orderWarningCacheService.invalidate(order.getTenantCode());
        }

        if (!Objects.equals(oldStatus, order.getStatus())
                || !Objects.equals(oldProcess, order.getProcess())
                || StringUtils.hasText(request.getRemark())) {
            insertProductionLog(order, oldStatusText, blankToNull(request.getRemark()));
        }
        syncLinkedSalesOrder(order, oldStatus);
        boolean logChanged = !Objects.equals(oldStatus, order.getStatus())
                || !Objects.equals(oldProcess, order.getProcess())
                || StringUtils.hasText(request.getRemark());
        if (!businessChanged && !logChanged) {
            OperationLogSkipContext.skipCurrent();
        }
    }

    /**
     * 旧的轻量状态维护接口继续保留，避免已有调用链路失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductionOrder(String orderId, ProductionOrderUpdateRequest request) {
        ProductionOrder order = findProductionOrder(orderId);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        String oldStatus = order.getStatus();
        Integer oldProcess = order.getProcess();
        String targetStatus = resolveProductionUpdateStatus(oldStatus, request.getStatus());
        validateProductionStatusForward(oldStatus, targetStatus);
        Integer targetProcess = resolveProductionUpdateProcess(order, targetStatus, request.getProcess());
        boolean changed = false;

        if (!Objects.equals(order.getStatus(), targetStatus)) {
            order.setStatus(targetStatus);
            changed = true;
        }
        if (!Objects.equals(oldProcess, targetProcess)) {
            order.setProcess(targetProcess);
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
        order.setBrandName(blankToNull(request.getBrandName()));
        String orderCategory = OrderCategoryEnum.normalize(request.getOrderCategory());
        order.setOrderCategory(orderCategory);
        order.setDeliveryDate(requireText(request.getDeliveryDate(), "销售订单交付日期不能为空"));
        order.setExpressCompany(blankToNull(request.getExpressCompany()));
        order.setExpressNo(blankToNull(request.getExpressNo()));
        order.setIsInvoice(normalizeInvoiceFlag(request.getIsInvoice()));
        order.setRemark(blankToNull(request.getRemark()));
        order.setAttachmentName(blankToNull(request.getAttachmentName()));
        order.setAttachmentUrl(normalizeSalesOrderAttachmentUrlForStorage(request.getAttachmentUrl()));
        order.setAttachmentSize(request.getAttachmentSize());
        order.setStatus(resolveSalesStatusForCategory(orderCategory, request.getStatus(), order.getStatus()));
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
            productionOrder.setBrandName(order.getBrandName());
            productionOrder.setOrderCategory(order.getOrderCategory());
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
        order.setBrandName(blankToNull(request.getBrandName()));
        order.setOrderCategory(OrderCategoryEnum.normalize(request.getOrderCategory()));
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

    private void enqueueLinkedProductionOrderFlowPrintTasksIfApproved(SalesOrder salesOrder) {
        if (!STATUS_PRODUCING.equals(salesOrder.getStatus())) {
            return;
        }
        List<ProductionOrder> linkedOrders = productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, salesOrder.getTenantCode())
                .eq(ProductionOrder::getSalesOrderId, salesOrder.getOrderId()));
        for (ProductionOrder linkedOrder : linkedOrders) {
            enqueueProductionOrderFlowPrintTaskIfAbsent(linkedOrder, "订单审批通过，自动生成生产订单流转码待打印任务");
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

    private String requireOrderId(OrderFlowPrintTaskRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new BusinessException("订单号不能为空");
        }
        return request.getOrderId().trim();
    }

    private Map<String, Object> buildSalesOrderFlowPrintPayload(SalesOrder order) {
        SalesOrderDetail firstItem = salesOrderDetailMapper.selectOne(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SalesOrderDetail::getOrderId, order.getOrderId())
                .orderByAsc(SalesOrderDetail::getId)
                .last("LIMIT 1"));
        Map<String, Object> payload = baseOrderFlowPrintPayload(
                order.getOrderId(),
                "sales",
                "订单流转",
                order.getStatus(),
                order.getOrderCategory(),
                order.getCustomerName(),
                order.getProjectName(),
                order.getBrandName(),
                firstItem == null ? order.getGoodsDesc() : firstItem.getModelCode());
        payload.put("customerPhone", safePrintText(order.getCustomerPhone(), ""));
        payload.put("deliveryDate", safePrintText(order.getDeliveryDate(), ""));
        payload.put("printReason", "订单流转码待打印");
        payload.put("flowQrPayload", buildOrderFlowQrText(payload));
        return payload;
    }

    private Map<String, Object> buildProductionOrderFlowPrintPayload(ProductionOrder order) {
        Map<String, Object> payload = baseOrderFlowPrintPayload(
                order.getOrderId(),
                "production",
                "订单流转",
                order.getStatus(),
                order.getOrderCategory(),
                order.getCustomerName(),
                order.getProjectName(),
                order.getBrandName(),
                order.getModelCode());
        payload.put("salesOrderId", safePrintText(order.getSalesOrderId(), ""));
        payload.put("process", order.getProcess());
        payload.put("processText", productionProcessLabel(order.getProcess()));
        payload.put("deliveryDate", order.getDeliveryDate() == null ? "" : DATE_TIME_FORMATTER.format(order.getDeliveryDate()));
        payload.put("printReason", "订单流转码待打印");
        payload.put("flowQrPayload", buildOrderFlowQrText(payload));
        return payload;
    }

    private Map<String, Object> baseOrderFlowPrintPayload(String orderId,
                                                          String orderType,
                                                          String orderTypeLabel,
                                                          String currentStatus,
                                                          String orderCategory,
                                                          String customerName,
                                                          String projectName,
                                                          String brandName,
                                                          String modelCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String safeOrderId = safePrintText(orderId, "");
        String safeCategory = normalizeOrderCategory(orderCategory);
        String flowCode = OrderFlowCodeUtil.generateFlowCode(
                orderFlowCodeSecret,
                TenantPermissionContext.getTenantCode(),
                orderType,
                safeOrderId
        );
        String flowScanCode = OrderFlowCodeUtil.buildScanCode(orderType, flowCode, safeOrderId);
        payload.put("orderId", safeOrderId);
        payload.put("barcode", safeOrderId);
        payload.put("flowCode", flowCode);
        payload.put("flowScanCode", flowScanCode);
        payload.put("flowBarcode", flowScanCode);
        payload.put("orderType", orderType);
        payload.put("orderTypeLabel", orderTypeLabel);
        payload.put("currentStatus", safePrintText(currentStatus, ""));
        payload.put("currentStatusText", orderStatusLabel(currentStatus));
        payload.put("orderCategory", safeCategory);
        payload.put("orderCategoryLabel", orderCategoryLabel(safeCategory));
        payload.put("customerName", safePrintText(customerName, "未填写客户"));
        payload.put("projectName", safePrintText(projectName, "未填写项目"));
        payload.put("brandName", safePrintText(brandName, "未填写品牌"));
        payload.put("modelCode", safePrintText(modelCode, "未填写型号"));
        payload.put("printDate", LocalDate.now().toString());
        payload.put("generatedAt", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        return payload;
    }

    private String buildOrderFlowQrText(Map<String, Object> payload) {
        return stringValue(payload.get("flowScanCode"));
    }

    private OrderFlowPrintTaskVO buildOrderFlowPrintTaskVO(String taskNo,
                                                           String orderId,
                                                           String orderType,
                                                           Map<String, Object> payload) {
        OrderFlowPrintTaskVO vo = new OrderFlowPrintTaskVO();
        vo.setTaskNo(taskNo);
        vo.setOrderId(orderId);
        vo.setOrderType(orderType);
        vo.setPrintType("order_flow");
        vo.setPrintPayload(payload);
        return vo;
    }

    private String orderStatusLabel(String status) {
        return switch (StringUtils.hasText(status) ? status.trim() : "") {
            case "pending_confirm" -> "待确认";
            case "pending_pay" -> "待收款";
            case "pending_material" -> "备料中";
            case "budgeting" -> "预算中";
            case "budget_completed" -> "预算完成";
            case "producing" -> "生产中";
            case "pending_ship" -> "待发货";
            case "shipped" -> "已发货";
            case "completed" -> "已完成";
            case "cancelled" -> "已取消";
            default -> "扫码识别";
        };
    }

    private String orderCategoryLabel(String category) {
        return switch (normalizeOrderCategory(category)) {
            case "sample_room" -> "样板间";
            case "replenishment" -> "补单";
            case "drawing_budget" -> "图纸预算";
            default -> "大货";
        };
    }

    private String normalizeOrderCategory(String category) {
        return OrderCategoryEnum.normalize(category);
    }

    private String resolveProductionUpdateStatus(String currentStatus, String requestedStatus) {
        String targetStatus = StringUtils.hasText(requestedStatus) ? requestedStatus.trim() : currentStatus;
        if (!StringUtils.hasText(targetStatus)) {
            targetStatus = OrderStatusEnum.PENDING_CONFIRM.getCode();
        }
        if (!PRODUCTION_STATUS_CODES.contains(targetStatus)) {
            throw new BusinessException("生产订单状态不合法");
        }
        return targetStatus;
    }

    private void validateProductionStatusForward(String currentStatus, String targetStatus) {
        String normalizedCurrent = StringUtils.hasText(currentStatus) ? currentStatus.trim() : OrderStatusEnum.PENDING_CONFIRM.getCode();
        if (Objects.equals(normalizedCurrent, targetStatus)) {
            return;
        }
        int currentIndex = PRODUCTION_STATUS_CODES.indexOf(normalizedCurrent);
        int targetIndex = PRODUCTION_STATUS_CODES.indexOf(targetStatus);
        if (currentIndex < 0 || targetIndex < 0) {
            throw new BusinessException("生产订单状态不合法");
        }
        if (targetIndex <= currentIndex) {
            throw new BusinessException("生产订单状态不能回退");
        }
        if (targetIndex > currentIndex + 1) {
            throw new BusinessException("生产订单状态不能跳级流转");
        }
    }

    private Integer resolveProductionUpdateProcess(ProductionOrder order, String targetStatus, Integer requestedProcess) {
        Integer currentProcess = order.getProcess();
        String currentStatus = StringUtils.hasText(order.getStatus()) ? order.getStatus().trim() : OrderStatusEnum.PENDING_CONFIRM.getCode();
        if (!STATUS_PRODUCING.equals(targetStatus)) {
            if (STATUS_PRODUCING.equals(currentStatus) && STATUS_PENDING_SHIP.equals(targetStatus)) {
                int process = requestedProcess == null ? (currentProcess == null ? -1 : currentProcess) : validateProductionProcessRange(requestedProcess);
                if (process < PRODUCTION_PROCESS_LABELS.size() - 1) {
                    throw new BusinessException("请先完成成品发货工序");
                }
                return process;
            }
            if (requestedProcess != null && !Objects.equals(currentProcess, requestedProcess)) {
                throw new BusinessException("只有生产中订单可以更新工序");
            }
            return currentProcess;
        }

        if (!STATUS_PRODUCING.equals(currentStatus)) {
            if (requestedProcess == null) {
                return null;
            }
            int process = validateProductionProcessRange(requestedProcess);
            if (process > 0) {
                throw new BusinessException("生产工序必须从原料入库开始");
            }
            return process;
        }

        int current = currentProcess == null ? -1 : currentProcess;
        if (requestedProcess == null) {
            return currentProcess;
        }
        int target = validateProductionProcessRange(requestedProcess);
        if (target < current) {
            throw new BusinessException("生产工序不能回退");
        }
        if (target > current + 1) {
            throw new BusinessException("生产工序不能跳级流转");
        }
        return target;
    }

    private int validateProductionProcessRange(Integer process) {
        if (process == null || process < 0 || process >= PRODUCTION_PROCESS_LABELS.size()) {
            throw new BusinessException("生产工序不合法");
        }
        return process;
    }

    private String safePrintText(Object value, String fallback) {
        String text = stringValue(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String toSimpleJson(Map<String, Object> source) {
        return source.entrySet().stream()
                .map(entry -> "\"" + jsonEscape(entry.getKey()) + "\":\"" + jsonEscape(stringValue(entry.getValue())) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String jsonEscape(String value) {
        return stringValue(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
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
        if (!StringUtils.hasText(status) || !OrderStatusEnum.PRODUCING.matches(status)) {
            return null;
        }
        if (process == null) {
            return null;
        }
        if (process < 0 || process >= PRODUCTION_PROCESS_LABELS.size()) {
            throw new BusinessException("生产工序不合法");
        }
        return process;
    }

    private String defaultSalesStatus(String currentStatus) {
        return OrderStatusEnum.defaultIfBlank(currentStatus, OrderStatusEnum.PENDING_CONFIRM);
    }

    private String resolveSalesStatusForCategory(String orderCategory, String requestedStatus, String currentStatus) {
        String category = normalizeOrderCategory(orderCategory);
        String requested = StringUtils.hasText(requestedStatus) ? requestedStatus.trim() : "";
        if (isDrawingBudgetOrder(category)) {
            if (!StringUtils.hasText(requested) || STATUS_PENDING_CONFIRM.equals(requested)) {
                return isBudgetStatus(currentStatus) ? currentStatus.trim() : STATUS_BUDGETING;
            }
            if (!isBudgetStatus(requested)) {
                throw new BusinessException("图纸预算订单只能使用预算中或预算完成状态");
            }
            return requested;
        }
        if (isBudgetStatus(requested)) {
            throw new BusinessException("只有图纸预算订单可以使用预算状态");
        }
        return StringUtils.hasText(requested) ? requested : defaultSalesStatus(currentStatus);
    }

    private boolean isDrawingBudgetOrder(String orderCategory) {
        return CATEGORY_DRAWING_BUDGET.equals(normalizeOrderCategory(orderCategory));
    }

    private boolean isBudgetStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim();
        return STATUS_BUDGETING.equals(normalized) || STATUS_BUDGET_COMPLETED.equals(normalized);
    }

    private String defaultProductionStatus(String currentStatus) {
        return OrderStatusEnum.defaultIfBlank(currentStatus, OrderStatusEnum.PENDING_CONFIRM);
    }

    private String buildProductionStateText(String status, Integer process) {
        if (!StringUtils.hasText(status)) {
            return "未设置";
        }
        if (process == null) {
            return orderStatusLabel(status);
        }
        String processLabel = productionProcessLabel(process);
        return StringUtils.hasText(processLabel) ? orderStatusLabel(status) + " / " + processLabel : orderStatusLabel(status);
    }

    private String productionProcessLabel(Integer process) {
        if (process == null || process < 0 || process >= PRODUCTION_PROCESS_LABELS.size()) {
            return "";
        }
        return PRODUCTION_PROCESS_LABELS.get(process);
    }

    private void fillProductionProcessView(ProductionOrderPageVO vo, String status, Integer process) {
        ProductionProcessView view = buildProductionProcessView(status, process);
        vo.setProcessText(view.processText());
        vo.setCurrentProcessText(view.currentProcessText());
        vo.setCompletedProcessText(view.completedProcessText());
        vo.setProcessProgressPercent(view.progressPercent());
        vo.setProcessSteps(view.steps());
    }

    private void fillProductionProcessView(ProductionOrderDetailVO vo, String status, Integer process) {
        ProductionProcessView view = buildProductionProcessView(status, process);
        vo.setProcessText(view.processText());
        vo.setCurrentProcessText(view.currentProcessText());
        vo.setCompletedProcessText(view.completedProcessText());
        vo.setProcessProgressPercent(view.progressPercent());
        vo.setProcessSteps(view.steps());
    }

    private ProductionProcessView buildProductionProcessView(String status, Integer process) {
        int total = PRODUCTION_PROCESS_LABELS.size();
        int completedIndex = resolveCompletedProcessIndex(status, process, total);
        int currentIndex = resolveCurrentProcessIndex(status, completedIndex, total);
        List<ProductionProcessStepVO> steps = java.util.stream.IntStream.range(0, total)
                .mapToObj(index -> {
                    ProductionProcessStepVO step = new ProductionProcessStepVO();
                    step.setCode(index);
                    step.setName(PRODUCTION_PROCESS_LABELS.get(index));
                    step.setDone(completedIndex >= index);
                    step.setCurrent(currentIndex == index);
                    return step;
                })
                .toList();
        String completedText = completedIndex >= 0 ? PRODUCTION_PROCESS_LABELS.get(completedIndex) : "";
        String currentText = currentIndex >= 0 ? PRODUCTION_PROCESS_LABELS.get(currentIndex) : "";
        if (STATUS_PRODUCING.equals(status) && currentIndex < 0) {
            currentText = "生产工序已完成";
        }
        if (!STATUS_PRODUCING.equals(status) && currentIndex < 0 && completedIndex >= total - 1) {
            currentText = "生产工序已完成";
        }
        String processText = StringUtils.hasText(currentText) ? currentText : completedText;
        int progressPercent = total <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(((completedIndex + 1) * 100f) / total)));
        return new ProductionProcessView(processText, currentText, completedText, progressPercent, steps);
    }

    private int resolveCompletedProcessIndex(String status, Integer process, int total) {
        if (total <= 0) {
            return -1;
        }
        if (OrderStatusEnum.PENDING_SHIP.matches(status)
                || OrderStatusEnum.SHIPPED.matches(status)
                || OrderStatusEnum.COMPLETED.matches(status)) {
            return total - 1;
        }
        if (!STATUS_PRODUCING.equals(status)) {
            return -1;
        }
        if (process == null) {
            return -1;
        }
        return Math.max(-1, Math.min(total - 1, process));
    }

    private int resolveCurrentProcessIndex(String status, int completedIndex, int total) {
        if (!STATUS_PRODUCING.equals(status) || total <= 0) {
            return -1;
        }
        int nextIndex = completedIndex + 1;
        return nextIndex >= 0 && nextIndex < total ? nextIndex : -1;
    }

    private record ProductionProcessView(
            String processText,
            String currentProcessText,
            String completedProcessText,
            Integer progressPercent,
            List<ProductionProcessStepVO> steps
    ) {
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

    private SalesOrder copySalesOrder(SalesOrder source) {
        SalesOrder copy = new SalesOrder();
        BeanUtils.copyProperties(source, copy);
        return copy;
    }

    private ProductionOrder copyProductionOrder(ProductionOrder source) {
        ProductionOrder copy = new ProductionOrder();
        BeanUtils.copyProperties(source, copy);
        return copy;
    }

    private boolean salesOrderContentChanged(SalesOrder before, SalesOrder after) {
        return !Objects.equals(before.getStatus(), after.getStatus())
                || !sameText(before.getCustomerName(), after.getCustomerName())
                || !sameText(before.getCustomerPhone(), after.getCustomerPhone())
                || !sameText(before.getProjectName(), after.getProjectName())
                || !sameText(before.getBrandName(), after.getBrandName())
                || !sameText(before.getGoodsDesc(), after.getGoodsDesc())
                || !sameBigDecimal(before.getTotalAmount(), after.getTotalAmount())
                || !Objects.equals(before.getTotalQuantity(), after.getTotalQuantity())
                || !sameText(before.getDeliveryDate(), after.getDeliveryDate())
                || !sameText(before.getExpressCompany(), after.getExpressCompany())
                || !sameText(before.getExpressNo(), after.getExpressNo())
                || !Objects.equals(before.getIsInvoice(), after.getIsInvoice())
                || !sameText(before.getRemark(), after.getRemark())
                || !sameText(before.getAttachmentName(), after.getAttachmentName())
                || !sameText(before.getAttachmentUrl(), after.getAttachmentUrl())
                || !Objects.equals(before.getAttachmentSize(), after.getAttachmentSize());
    }

    private boolean productionOrderContentChanged(ProductionOrder before, ProductionOrder after) {
        return !sameText(before.getSalesOrderId(), after.getSalesOrderId())
                || !Objects.equals(before.getStatus(), after.getStatus())
                || !sameText(before.getModelCode(), after.getModelCode())
                || !sameText(before.getFabric(), after.getFabric())
                || !sameBigDecimal(before.getWeight(), after.getWeight())
                || !sameBigDecimal(before.getWidth(), after.getWidth())
                || !sameText(before.getColor(), after.getColor())
                || !Objects.equals(before.getQuantity(), after.getQuantity())
                || !sameBigDecimal(before.getPrice(), after.getPrice())
                || !sameBigDecimal(before.getTotalAmount(), after.getTotalAmount())
                || !Objects.equals(before.getProcess(), after.getProcess())
                || !sameText(before.getCustomerName(), after.getCustomerName())
                || !sameText(before.getProjectName(), after.getProjectName())
                || !sameText(before.getBrandName(), after.getBrandName())
                || !sameText(before.getContactPhone(), after.getContactPhone())
                || !Objects.equals(before.getDeliveryDate(), after.getDeliveryDate());
    }

    private boolean salesOrderItemsChanged(String orderId, List<SalesOrderSaveRequest.ItemDTO> items) {
        List<SalesOrderDetail> existingItems = salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId)
                .orderByAsc(SalesOrderDetail::getId));
        List<SalesOrderSaveRequest.ItemDTO> requestItems = items == null ? Collections.emptyList() : items;
        if (existingItems.size() != requestItems.size()) {
            return true;
        }
        for (int i = 0; i < existingItems.size(); i++) {
            SalesOrderDetail existing = existingItems.get(i);
            SalesOrderSaveRequest.ItemDTO request = requestItems.get(i);
            if (!sameText(existing.getModelCode(), request.getModelCode())
                    || !sameFloat(existing.getWeight(), request.getWeight())
                    || !sameText(existing.getSpec(), formatNumber(request.getSpec()))
                    || !sameBigDecimal(existing.getQuantity(), request.getQuantity())) {
                return true;
            }
        }
        return false;
    }

    private boolean sameText(String left, String right) {
        return Objects.equals(blankToNull(left), blankToNull(right));
    }

    private boolean sameFloat(Float left, Float right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return BigDecimal.valueOf(left.doubleValue()).compareTo(BigDecimal.valueOf(right.doubleValue())) == 0;
    }

    private boolean sameBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.compareTo(right) == 0;
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

    private void applySalesStaleWarningFilter(LambdaQueryWrapper<SalesOrder> wrapper, String tenantCode) {
        LocalDateTime now = LocalDateTime.now();
        wrapper.notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(), OrderStatusEnum.CANCELLED.getCode(), STATUS_BUDGET_COMPLETED)
                .and(category -> category
                        .and(sample -> sample.eq(SalesOrder::getOrderCategory, CATEGORY_SAMPLE_ROOM)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_SAMPLE_ROOM))))
                        .or(bulk -> bulk.eq(SalesOrder::getOrderCategory, CATEGORY_BULK)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_BULK))))
                        .or(replenishment -> replenishment.eq(SalesOrder::getOrderCategory, CATEGORY_REPLENISHMENT)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_REPLENISHMENT))))
                        .or(drawingBudget -> drawingBudget.eq(SalesOrder::getOrderCategory, CATEGORY_DRAWING_BUDGET)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_DRAWING_BUDGET))))
                        .or(other -> other.and(fallback -> fallback.isNull(SalesOrder::getOrderCategory)
                                        .or()
                                        .notIn(SalesOrder::getOrderCategory, CATEGORY_SAMPLE_ROOM, CATEGORY_BULK,
                                                CATEGORY_REPLENISHMENT, CATEGORY_DRAWING_BUDGET))
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode)))));
    }

    private void applyProductionStaleWarningFilter(LambdaQueryWrapper<ProductionOrder> wrapper, String tenantCode) {
        LocalDateTime now = LocalDateTime.now();
        wrapper.ne(ProductionOrder::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .and(category -> category
                        .and(sample -> sample.eq(ProductionOrder::getOrderCategory, CATEGORY_SAMPLE_ROOM)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_SAMPLE_ROOM))))
                        .or(bulk -> bulk.eq(ProductionOrder::getOrderCategory, CATEGORY_BULK)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_BULK))))
                        .or(replenishment -> replenishment.eq(ProductionOrder::getOrderCategory, CATEGORY_REPLENISHMENT)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_REPLENISHMENT))))
                        .or(drawingBudget -> drawingBudget.eq(ProductionOrder::getOrderCategory, CATEGORY_DRAWING_BUDGET)
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode, CATEGORY_DRAWING_BUDGET))))
                        .or(other -> other.and(fallback -> fallback.isNull(ProductionOrder::getOrderCategory)
                                        .or()
                                        .notIn(ProductionOrder::getOrderCategory, CATEGORY_SAMPLE_ROOM, CATEGORY_BULK,
                                                CATEGORY_REPLENISHMENT, CATEGORY_DRAWING_BUDGET))
                                .apply("COALESCE(update_time, create_time) <= {0}",
                                        now.minusDays(orderSettingService.staleWarningDays(tenantCode)))));
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
        return !OrderStatusEnum.COMPLETED.matches(status)
                && !OrderStatusEnum.CANCELLED.matches(status)
                && !STATUS_BUDGET_COMPLETED.equals(status);
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
