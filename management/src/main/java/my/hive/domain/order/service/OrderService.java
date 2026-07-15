package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.context.OperationLogSkipContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.order.OrderFlowCodeUtil;
import my.hive.domain.print.PrintTaskService;
import my.hive.shared.enums.BinaryFlagEnum;
import my.hive.shared.security.InternalUploadUrlValidator;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.entity.CustomerProject;
import my.hive.domain.customer.model.enums.CustomerTypeEnum;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.installation.service.InstallationTaskService;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.ProductionOrderStatusLogMapper;
import my.hive.domain.order.mapper.SalesOrderDetailMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.ProductionOrderPageRequest;
import my.hive.domain.order.model.dto.ProductionOrderSaveRequest;
import my.hive.domain.order.model.dto.ProductionOrderUpdateRequest;
import my.hive.domain.order.model.dto.OrderFlowPrintTaskRequest;
import my.hive.domain.order.model.dto.OrderStatusLogTimeCorrectionRequest;
import my.hive.domain.order.model.dto.OrderWarningSettingUpdateRequest;
import my.hive.domain.order.model.dto.SalesOrderPageRequest;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.entity.ProductionOrder;
import my.hive.domain.order.model.entity.ProductionOrderStatusLog;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderDetail;
import my.hive.domain.order.model.entity.SalesOrderStatusLog;
import my.hive.domain.order.model.enums.OrderCategoryEnum;
import my.hive.domain.order.model.enums.OrderLogOperateTypeEnum;
import my.hive.domain.order.model.enums.OrderStatusEnum;
import my.hive.domain.order.model.vo.ProductionOrderDetailVO;
import my.hive.domain.order.model.vo.ProductionOrderPageVO;
import my.hive.domain.order.model.vo.ProductionProcessStepVO;
import my.hive.domain.order.model.vo.ProductionOrderStatusLogVO;
import my.hive.domain.order.model.vo.OrderFlowPrintTaskVO;
import my.hive.domain.order.model.vo.OrderWarningSettingVO;
import my.hive.domain.order.model.vo.OrderWarningSummaryVO;
import my.hive.domain.order.model.vo.SalesOrderAttachmentVO;
import my.hive.domain.order.model.vo.SalesOrderDetailVO;
import my.hive.domain.order.model.vo.SalesOrderPageVO;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.domain.order.model.vo.SalesOrderStatusLogVO;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
            "pending_confirm", "pending_pay", "pending_material", "producing", "pending_ship", "shipped", "completed", "pending_cancel", "cancelled"
    );
    private static final List<String> PRODUCTION_STATUS_CODES = List.of(
            "pending_confirm", "pending_pay", "pending_material", "producing", "pending_ship", "shipped", "completed"
    );
    private static final List<String> PRODUCTION_PROCESS_LABELS = List.of(
            "原料入库", "原料检验", "尺寸裁剪", "窗帘缝制", "窗帘熨烫",
            "成品检验", "高温定型", "打包装箱", "成品入库", "成品发货"
    );
    private static final String STATUS_PENDING_PAY = OrderStatusEnum.PENDING_PAY.getCode();
    private static final String STATUS_PENDING_CONFIRM = OrderStatusEnum.PENDING_CONFIRM.getCode();
    private static final String STATUS_BUDGETING = OrderStatusEnum.BUDGETING.getCode();
    private static final String STATUS_BUDGET_COMPLETED = OrderStatusEnum.BUDGET_COMPLETED.getCode();
    private static final String STATUS_PENDING_MATERIAL = OrderStatusEnum.PENDING_MATERIAL.getCode();
    private static final String STATUS_PRODUCING = OrderStatusEnum.PRODUCING.getCode();
    private static final String STATUS_PENDING_SHIP = OrderStatusEnum.PENDING_SHIP.getCode();
    private static final String STATUS_SHIPPED = OrderStatusEnum.SHIPPED.getCode();
    private static final String STATUS_PENDING_CANCEL = OrderStatusEnum.PENDING_CANCEL.getCode();
    private static final String STATUS_CANCELLED = OrderStatusEnum.CANCELLED.getCode();
    private static final String CATEGORY_DRAWING_BUDGET = OrderCategoryEnum.DRAWING_BUDGET.getCode();
    private static final String CATEGORY_SAMPLE_ROOM = OrderCategoryEnum.SAMPLE_ROOM.getCode();
    private static final String CATEGORY_BULK = OrderCategoryEnum.BULK.getCode();
    private static final String CATEGORY_REPLENISHMENT = OrderCategoryEnum.REPLENISHMENT.getCode();
    private static final String CATEGORY_SPECIAL_ORDER = OrderCategoryEnum.SPECIAL_ORDER.getCode();
    private static final String APPROVAL_TYPE_ORDER = "ORDER";
    private static final String ORDER_TYPE_SALES = "sales";
    private static final String ORDER_TYPE_PRODUCTION = "production";
    private static final int MAX_PARALLEL_APPROVERS = 8;
    private static final List<String> SALES_FORWARD_STATUS_CODES = List.of(
            "pending_confirm", "pending_pay", "pending_material", "producing", "pending_ship", "shipped", "completed"
    );
    private static final List<String> SALES_BUDGET_FORWARD_STATUS_CODES = List.of(
            "budgeting", "budget_completed"
    );
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
    private ApprovalAuditorCandidateService approvalAuditorCandidateService;

    @Resource
    private ApprovalDefaultAuditorService approvalDefaultAuditorService;

    @Resource
    private PrintTaskService printTaskService;

    @Resource
    private InstallationTaskService installationTaskService;

    @Resource
    private OrderNoteService orderNoteService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${ORDER_FLOW_CODE_SECRET:${AUTH_TOKEN_SECRET:hive-local-order-flow-secret}}")
    private String orderFlowCodeSecret;

    public OrderFlowPrintTaskVO createSalesOrderFlowPrintTask(OrderFlowPrintTaskRequest request) {
        String orderId = requireOrderId(request);
        SalesOrder order = findSalesOrder(orderId);
        assertSalesOrderStagePermission(order, order.getStatus());
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

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public SalesOrderAttachmentVO uploadSalesAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的订单附件");
        }
        if (file.getSize() <= 0) {
            throw new BusinessException("订单附件内容为空，无法上传");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException("订单附件不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename() == null ? "attachment" : StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
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
                .orderByDesc(SalesOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(SalesOrder::getStatus, request.getStatus().trim());
        }
        Set<String> permittedStatuses = permittedOrderStatuses(SALES_STATUS_CODES);
        applyOrderStatusPermissionFilter(wrapper, SalesOrder::getStatus, permittedStatuses);
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
        if (StringUtils.hasText(request.getInformationChannel())) {
            wrapper.like(SalesOrder::getInformationChannel, request.getInformationChannel().trim());
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
        Map<String, List<SalesOrderDetail>> detailMap = buildSalesDetailMap(orders);
        Map<String, List<ProductionOrder>> fulfillmentMap = buildFulfillmentMap(orders);

        List<SalesOrderPageVO> records = orders.stream().map(order -> {
            SalesOrderPageVO vo = new SalesOrderPageVO();
            BeanUtils.copyProperties(order, vo);
            List<SalesOrderDetail> details = detailMap.getOrDefault(order.getOrderId(), Collections.emptyList());
            vo.setDetailCount(details.size());
            vo.setItems(details.stream().map(this::toSalesOrderPageItem).toList());
            applyFulfillmentView(vo, fulfillmentMap.getOrDefault(order.getOrderId(), Collections.emptyList()), order.getStatus());
            markSalesStaleWarning(vo, order, orderSettingService.staleWarningDays(tenantCode, order.getOrderCategory()));
            return vo;
        }).toList();

        Page<SalesOrderPageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    public Map<String, Long> countSalesOrderStatuses() {
        Set<String> permittedStatuses = permittedOrderStatuses(SALES_STATUS_CODES);
        Map<String, Long> result = new LinkedHashMap<>();
        long total = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)));
        result.put("total", total);
        for (String status : SALES_STATUS_CODES) {
            result.put(status, safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                    .eq(SalesOrder::getStatus, status))));
        }
        long sampleRoomCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.SAMPLE_ROOM.getCode())));
        long replenishmentCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.REPLENISHMENT.getCode())));
        long drawingBudgetCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.DRAWING_BUDGET.getCode())));
        long specialOrderCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.SPECIAL_ORDER.getCode())));
        long bulkCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getOrderCategory, OrderCategoryEnum.BULK.getCode())));
        long invoicePaidCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getIsInvoice, BinaryFlagEnum.YES.getCode())));
        long invoiceUnpaidCount = safeCount(salesOrderMapper.selectCount(scopedSalesOrderWrapper(permittedStatuses)
                .eq(SalesOrder::getIsInvoice, BinaryFlagEnum.NO.getCode())));
        result.put("category_sample_room", sampleRoomCount);
        result.put("category_replenishment", replenishmentCount);
        result.put("category_drawing_budget", drawingBudgetCount);
        result.put("category_special_order", specialOrderCount);
        result.put("category_bulk", bulkCount);
        result.put("invoice_paid", invoicePaidCount);
        result.put("invoice_unpaid", invoiceUnpaidCount);
        return result;
    }

    public Set<String> currentPermittedOrderStatuses() {
        Set<String> permittedStatuses = permittedOrderStatuses(SALES_STATUS_CODES);
        return permittedStatuses == null
                ? new LinkedHashSet<>(SALES_STATUS_CODES)
                : new LinkedHashSet<>(permittedStatuses);
    }

    public SalesOrderDetailVO getSalesOrderDetail(String orderId) {
        SalesOrder order = findSalesOrder(orderId);
        assertSalesOrderStagePermission(order, order.getStatus());
        List<SalesOrderDetail> details = salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId)
                .orderByAsc(SalesOrderDetail::getId));

        SalesOrderDetailVO vo = new SalesOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        Map<String, List<ProductionOrder>> fulfillmentMap = buildFulfillmentMap(List.of(order));
        applyFulfillmentView(vo, fulfillmentMap.getOrDefault(order.getOrderId(), Collections.emptyList()), order.getStatus());
        vo.setItems(details.stream().map(detail -> {
            SalesOrderDetailVO.ItemVO itemVO = new SalesOrderDetailVO.ItemVO();
            BeanUtils.copyProperties(detail, itemVO);
            return itemVO;
        }).toList());
        vo.setNotes(orderNoteService.listNotesIfPermitted(order.getTenantCode(), orderId));
        vo.setLogs(listSalesLogs(orderId));
        return vo;
    }

    public List<SalesOrderStatusLogVO> listSalesLogs(String orderId) {
        SalesOrder order = findSalesOrder(orderId);
        assertSalesOrderStagePermission(order, order.getStatus());
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
    public void correctSalesLogTime(Long logId, OrderStatusLogTimeCorrectionRequest request) {
        if (logId == null) {
            throw new BusinessException("流转记录不存在");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        SalesOrderStatusLog log = salesOrderStatusLogMapper.selectOne(new LambdaQueryWrapper<SalesOrderStatusLog>()
                .eq(SalesOrderStatusLog::getId, logId)
                .eq(SalesOrderStatusLog::getTenantCode, tenantCode)
                .last("LIMIT 1"));
        if (log == null) {
            throw new BusinessException("流转记录不存在或无权修正");
        }
        assertSalesLogStagePermission(log);
        LocalDateTime correctedTime = resolveRequiredBusinessTime(request.getCreateTime(), "流转记录时间");
        salesOrderStatusLogMapper.update(null, new LambdaUpdateWrapper<SalesOrderStatusLog>()
                .eq(SalesOrderStatusLog::getId, log.getId())
                .eq(SalesOrderStatusLog::getTenantCode, tenantCode)
                .set(SalesOrderStatusLog::getCreateTime, correctedTime));
        orderWarningCacheService.invalidate(tenantCode);
        OperationLogSkipContext.skipCurrent();
    }

    @Transactional(rollbackFor = Exception.class)
    public String createSalesOrder(SalesOrderSaveRequest request) {
        SalesOrder order = new SalesOrder();
        order.setOrderId(codeGeneratorUtil.generateSalesOrderCode());
        order.setTenantCode(TenantPermissionContext.getTenantCode());
        order.setCreator(resolveCurrentUser());
        applySalesOrderContent(order, request, true);
        assertSalesOrderStagePermission(order, order.getStatus());
        LocalDateTime businessCreateTime = resolveBusinessCreateTime(request.getCreateTime(), "销售订单录单时间");
        order.setCreateTime(businessCreateTime);
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.insert(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        replaceSalesOrderItems(order.getOrderId(), request.getItems(), businessCreateTime);
        orderNoteService.saveNotes(order.getTenantCode(), order.getOrderId(), order.getStatus(), request.getNotes());
        insertSalesLog(order, null, OrderLogOperateTypeEnum.CREATE.getCode(), "创建销售订单", businessCreateTime);

        if (BinaryFlagEnum.isYes(request.getCreateProductionOrder()) && canAutoCreateProductionOrder(order.getOrderCategory())) {
            createProductionOrdersFromSales(order, request.getItems());
        }
        syncInstallationTaskIfCompleted(order);
        return order.getOrderId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSalesOrder(String orderId, SalesOrderSaveRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        SalesOrder beforeOrder = copySalesOrder(order);
        String oldStatus = order.getStatus();
        boolean itemsChanged = salesOrderItemsChanged(orderId, request.getItems());
        applySalesOrderContent(order, request, false);
        validateSalesStatusForward(order.getOrderCategory(), oldStatus, order.getStatus());
        assertDirectSalesTransitionAllowed(order.getOrderCategory(), oldStatus, order.getStatus());
        assertOrderStatusTransitionPermission(oldStatus, order.getStatus());
        assertSalesOrderStagePermission(order, order.getStatus());
        applyManualCreateTime(order, request.getCreateTime(), "销售订单录单时间");
        boolean businessChanged = salesOrderContentChanged(beforeOrder, order) || itemsChanged;
        order.setUpdateTime(businessChanged ? LocalDateTime.now() : beforeOrder.getUpdateTime());
        salesOrderMapper.updateById(order);
        if (businessChanged) {
            orderWarningCacheService.invalidate(order.getTenantCode());
            replaceSalesOrderItems(orderId, request.getItems(), order.getCreateTime());
        }
        orderNoteService.saveNotes(order.getTenantCode(), orderId, order.getStatus(), request.getNotes());
        boolean logChanged = !Objects.equals(oldStatus, order.getStatus());
        if (logChanged) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(), "管理端更新订单状态");
        }
        syncLinkedProductionOrders(order, oldStatus);
        syncInstallationTaskIfCompleted(order);
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
            order.setStatus(resolveSalesStatusForCategory(order.getOrderCategory(), request.getStatus(), oldStatus, false));
        }
        validateSalesStatusForward(order.getOrderCategory(), oldStatus, order.getStatus());
        assertDirectSalesTransitionAllowed(order.getOrderCategory(), oldStatus, order.getStatus());
        assertOrderStatusTransitionPermission(oldStatus, order.getStatus());
        assertSalesOrderStagePermission(order, order.getStatus());
        if (request.getInformationChannel() != null) {
            order.setInformationChannel(blankToNull(request.getInformationChannel()));
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
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        if (!Objects.equals(oldStatus, order.getStatus()) || StringUtils.hasText(request.getRemark())) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(), blankToNull(request.getRemark()));
        }
        syncLinkedProductionOrders(order, oldStatus);
        syncInstallationTaskIfCompleted(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void advanceSalesOrderToNextStage(String orderId, SalesOrderUpdateRequest request) {
        SalesOrder order = findSalesOrderForUpdate(orderId);
        String oldStatus = order.getStatus();
        String targetStatus = resolveNextSalesStatus(order);
        assertOrderStatusActionPermission(oldStatus, "advance");
        assertSalesOrderStagePermission(order, targetStatus);
        if (isSalesTransitionApprovalRequired(oldStatus, targetStatus)) {
            submitSalesTransitionApproval(order, targetStatus, request);
            return;
        }
        order.setStatus(targetStatus);
        if (request != null) {
            if (request.getInformationChannel() != null) {
                order.setInformationChannel(blankToNull(request.getInformationChannel()));
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
        }
        validateSalesStatusForward(order.getOrderCategory(), oldStatus, order.getStatus());
        assertDirectSalesTransitionAllowed(order.getOrderCategory(), oldStatus, order.getStatus());
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(),
                request == null ? null : blankToNull(request.getRemark()));
        syncLinkedProductionOrders(order, oldStatus);
        syncInstallationTaskIfCompleted(order);
    }

    private boolean isSalesTransitionApprovalRequired(String oldStatus, String targetStatus) {
        return (STATUS_PENDING_PAY.equals(oldStatus) && STATUS_PENDING_MATERIAL.equals(targetStatus))
                || (STATUS_PENDING_SHIP.equals(oldStatus) && STATUS_SHIPPED.equals(targetStatus));
    }

    private void submitSalesTransitionApproval(SalesOrder order, String targetStatus, SalesOrderUpdateRequest request) {
        String approvalCode = orderApprovalCode(ORDER_TYPE_SALES, order.getOrderId());
        if (!approvalAuditorCandidateService.findPendingAuditorIds(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode).isEmpty()) {
            throw new BusinessException("该订单已有待处理审批，请审批完成后再操作");
        }
        if (STATUS_PENDING_PAY.equals(order.getStatus()) && STATUS_PENDING_MATERIAL.equals(targetStatus)) {
            submitSalesMaterialApproval(order, request);
            return;
        }
        if (request != null) {
            if (request.getInformationChannel() != null) {
                order.setInformationChannel(blankToNull(request.getInformationChannel()));
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
        }
        validateShippingInfo(targetStatus, order.getExpressCompany(), order.getExpressNo());
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        insertSalesLog(order, order.getStatus(), targetStatus, OrderLogOperateTypeEnum.APPROVAL_PENDING.getCode(),
                request == null ? null : blankToNull(request.getRemark()), LocalDateTime.now());
        replaceSalesOrderApprovalAuditors(order, request == null ? null : request.getAuditorIds());
    }

    private void submitSalesMaterialApproval(SalesOrder order, SalesOrderUpdateRequest request) {
        String remark = request == null ? null : blankToNull(request.getRemark());
        insertSalesLog(order, order.getStatus(), STATUS_PENDING_MATERIAL,
                OrderLogOperateTypeEnum.APPROVAL_PENDING.getCode(),
                StringUtils.hasText(remark) ? remark : "提交备料审批",
                LocalDateTime.now());
        replaceSalesOrderApprovalAuditors(order, request == null ? null : request.getAuditorIds());
    }

    private void replaceSalesOrderApprovalAuditors(SalesOrder order, List<Long> auditorIds) {
        String requiredPermission = orderAuditPermission(order);
        List<Long> selectedAuditorIds = normalizeApprovalAuditorIds(auditorIds);
        if (selectedAuditorIds.isEmpty()) {
            selectedAuditorIds = approvalDefaultAuditorService.resolveAuditorIds(
                    order.getTenantCode(),
                    APPROVAL_TYPE_ORDER,
                    TenantPermissionContext.getUserId(),
                    null,
                    null,
                    requiredPermission,
                    false);
        }
        if (selectedAuditorIds.isEmpty()) {
            throw new BusinessException("订单审批人不能为空");
        }
        List<Long> permittedIds = employeeMapper.selectActiveApproverIdsByPermission(
                order.getTenantCode(), requiredPermission);
        for (Long auditorId : selectedAuditorIds) {
            if (permittedIds == null || !permittedIds.contains(auditorId)) {
                throw new BusinessException("所选审批人没有订单审批权限");
            }
        }
        approvalAuditorCandidateService.replaceActiveCandidates(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, orderApprovalCode(ORDER_TYPE_SALES, order.getOrderId()), selectedAuditorIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitSalesOrderRollbackApproval(String orderId, SalesOrderUpdateRequest request) {
        SalesOrder order = findSalesOrder(orderId);
        String currentStatus = normalizeStatus(order.getStatus());
        String targetStatus = request != null && StringUtils.hasText(request.getStatus())
                ? request.getStatus().trim()
                : resolvePreviousSalesStatus(order);
        validateSalesRollbackTarget(order, currentStatus, targetStatus);
        assertOrderStatusActionPermission(currentStatus, "rollback");
        assertSalesOrderStagePermission(order, currentStatus);
        String approvalCode = orderApprovalCode(ORDER_TYPE_SALES, order.getOrderId());
        if (!approvalAuditorCandidateService.findPendingAuditorIds(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode).isEmpty()) {
            throw new BusinessException("该订单已有待处理审批，请审批完成后再操作");
        }
        List<Long> auditorIds = normalizeApprovalAuditorIds(request == null ? null : request.getAuditorIds());
        String requiredPermission = PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
        if (auditorIds.isEmpty()) {
            auditorIds = approvalDefaultAuditorService.resolveAuditorIds(
                    order.getTenantCode(),
                    APPROVAL_TYPE_ORDER,
                    TenantPermissionContext.getUserId(),
                    null,
                    null,
                    requiredPermission,
                    false);
        }
        List<Long> permittedIds = employeeMapper.selectActiveApproverIdsByPermission(
                order.getTenantCode(), requiredPermission);
        for (Long auditorId : auditorIds) {
            if (permittedIds == null || !permittedIds.contains(auditorId)) {
                throw new BusinessException("所选审批人没有订单审批权限");
            }
        }
        String remark = request == null ? null : blankToNull(request.getRemark());
        insertSalesLog(order, currentStatus, targetStatus, OrderLogOperateTypeEnum.ROLLBACK_PENDING.getCode(),
                StringUtils.hasText(remark)
                        ? remark
                        : "提交订单回退审批：" + orderStatusLabel(currentStatus) + " → " + orderStatusLabel(targetStatus),
                LocalDateTime.now());
        approvalAuditorCandidateService.replaceActiveCandidates(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode, auditorIds);
    }

    private List<Long> normalizeApprovalAuditorIds(List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Long currentUserId = TenantPermissionContext.getUserId();
        for (Long auditorId : auditorIds) {
            if (auditorId == null || auditorId <= 0) {
                continue;
            }
            if (currentUserId != null && currentUserId.equals(auditorId)) {
                throw new BusinessException("审批人不能选择提交人本人");
            }
            ids.add(auditorId);
        }
        if (ids.size() > MAX_PARALLEL_APPROVERS) {
            throw new BusinessException("审批人不能超过 " + MAX_PARALLEL_APPROVERS + " 人");
        }
        return new ArrayList<>(ids);
    }

    private String orderApprovalCode(String orderType, String orderId) {
        String type = ORDER_TYPE_PRODUCTION.equalsIgnoreCase(orderType) ? ORDER_TYPE_PRODUCTION : ORDER_TYPE_SALES;
        return type + ":" + orderId.trim();
    }

    /**
     * 订单审批中心专用入口：只有审批通过后才允许待收款销售订单进入备料中。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveSalesOrderTransition(String orderId, String targetStatus, String remark) {
        if (!StringUtils.hasText(targetStatus)) {
            throw new BusinessException("目标订单状态不能为空");
        }
        SalesOrder order = findSalesOrder(orderId);
        String oldStatus = order.getStatus();
        order.setStatus(targetStatus.trim());
        validateSalesStatusForward(order.getOrderCategory(), oldStatus, order.getStatus());
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        if (!Objects.equals(oldStatus, order.getStatus())) {
            insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(),
                    StringUtils.hasText(remark) ? remark.trim() : "审批中心确认销售订单流转");
        }
        syncLinkedProductionOrders(order, oldStatus);
        syncInstallationTaskIfCompleted(order);
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            return;
        }
        enqueueSalesOrderFlowPrintTaskIfAbsent(order, "订单审批通过，自动生成销售订单流转码待打印任务");
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveSalesOrderRollback(String orderId, String remark) {
        SalesOrder order = findSalesOrder(orderId);
        SalesOrderStatusLog rollbackLog = findPendingSalesRollbackLog(order.getOrderId());
        if (rollbackLog == null) {
            throw new BusinessException("未找到待审批的订单回退申请");
        }
        String oldStatus = normalizeStatus(order.getStatus());
        String sourceStatus = normalizeStatus(rollbackLog.getOldStatus());
        String targetStatus = normalizeStatus(rollbackLog.getNewStatus());
        if (!Objects.equals(oldStatus, sourceStatus)) {
            throw new BusinessException("订单状态已变化，请重新提交回退审批");
        }
        validateSalesRollbackTarget(order, oldStatus, targetStatus);
        assertSalesOrderStagePermission(order, oldStatus);
        order.setStatus(targetStatus);
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.ROLLBACK_APPROVED.getCode(),
                StringUtils.hasText(remark) ? remark.trim() : "订单回退审批通过");
        syncLinkedProductionOrders(order, oldStatus);
        syncInstallationTaskIfCompleted(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectPendingCancelSalesOrder(String orderId, String remark) {
        SalesOrder order = findSalesOrder(orderId);
        if (!STATUS_PENDING_CANCEL.equals(order.getStatus())) {
            throw new BusinessException("当前订单不是取消审核中，不能驳回取消申请");
        }
        String oldStatus = order.getStatus();
        String restoredStatus = resolveStatusBeforePendingCancel(orderId);
        order.setStatus(restoredStatus);
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
        orderWarningCacheService.invalidate(order.getTenantCode());
        insertSalesLog(order, oldStatus, OrderLogOperateTypeEnum.STATUS_CHANGE.getCode(),
                StringUtils.hasText(remark) ? remark.trim() : "取消订单审核未通过，订单恢复原状态");
    }

    public Page<ProductionOrderPageVO> pageProductionOrders(ProductionOrderPageRequest request) {
        if (request == null) {
            request = new ProductionOrderPageRequest();
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        Page<ProductionOrder> page = new Page<>(safePage(request.getPageNum()), safeSize(request.getPageSize()));

        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<ProductionOrder>()
                .orderByDesc(ProductionOrder::getCreateTime);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ProductionOrder::getStatus, request.getStatus().trim());
        }
        Set<String> permittedStatuses = permittedOrderStatuses(PRODUCTION_STATUS_CODES);
        applyOrderStatusPermissionFilter(wrapper, ProductionOrder::getStatus, permittedStatuses);
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
        if (StringUtils.hasText(request.getInformationChannel())) {
            wrapper.like(ProductionOrder::getInformationChannel, request.getInformationChannel().trim());
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
        Set<String> permittedStatuses = permittedOrderStatuses(PRODUCTION_STATUS_CODES);
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", safeCount(productionOrderMapper.selectCount(scopedProductionOrderWrapper(permittedStatuses))));
        for (String status : PRODUCTION_STATUS_CODES) {
            result.put(status, safeCount(productionOrderMapper.selectCount(scopedProductionOrderWrapper(permittedStatuses)
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
        return orderWarningCacheService.summary(
                TenantPermissionContext.getTenantCode(), currentPermittedOrderStatuses());
    }

    public OrderWarningSummaryVO refreshOrderWarningSummary() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        orderWarningCacheService.invalidate(tenantCode);
        return orderWarningCacheService.summary(tenantCode, currentPermittedOrderStatuses());
    }

    @Transactional
    public OrderWarningSummaryVO refreshOrderWarnings() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .select(SalesOrder::getOrderId);
        applySalesStaleWarningFilter(wrapper, tenantCode);
        Set<String> permittedStatuses = currentPermittedOrderStatuses();
        applyOrderStatusPermissionFilter(wrapper, SalesOrder::getStatus, permittedStatuses);
        List<String> orderIds = salesOrderMapper.selectList(wrapper).stream()
                .map(SalesOrder::getOrderId)
                .filter(StringUtils::hasText)
                .toList();
        if (!orderIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (int start = 0; start < orderIds.size(); start += 500) {
                List<String> batch = orderIds.subList(start, Math.min(start + 500, orderIds.size()));
                salesOrderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                        .in(SalesOrder::getOrderId, batch)
                        .set(SalesOrder::getUpdateTime, now));
            }
        }
        orderWarningCacheService.invalidate(tenantCode);
        return orderWarningCacheService.summary(tenantCode, permittedStatuses);
    }

    @Transactional
    public OrderWarningSummaryVO refreshOrderWarning(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单编号不能为空");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        SalesOrder order = findSalesOrder(orderId.trim());
        salesOrderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderId, order.getOrderId())
                .set(SalesOrder::getUpdateTime, LocalDateTime.now()));
        orderWarningCacheService.invalidate(tenantCode);
        return orderWarningCacheService.summary(tenantCode, currentPermittedOrderStatuses());
    }

    public ProductionOrderDetailVO getProductionOrderDetail(String orderId) {
        ProductionOrder order = findProductionOrder(orderId);
        assertProductionOrderStagePermission(order, order.getStatus());
        List<ProductionOrderStatusLogVO> logs = listProductionLogs(orderId);

        ProductionOrderDetailVO vo = new ProductionOrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        fillProductionProcessView(vo, order.getStatus(), order.getProcess());
        vo.setLogs(logs);
        return vo;
    }

    public List<ProductionOrderStatusLogVO> listProductionLogs(String orderId) {
        ProductionOrder order = findProductionOrder(orderId);
        assertProductionOrderStagePermission(order, order.getStatus());
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
    public void correctProductionLogTime(Long logId, OrderStatusLogTimeCorrectionRequest request) {
        if (logId == null) {
            throw new BusinessException("流转记录不存在");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        ProductionOrderStatusLog log = productionOrderStatusLogMapper.selectOne(new LambdaQueryWrapper<ProductionOrderStatusLog>()
                .eq(ProductionOrderStatusLog::getId, logId)
                .eq(ProductionOrderStatusLog::getTenantCode, tenantCode)
                .last("LIMIT 1"));
        if (log == null) {
            throw new BusinessException("流转记录不存在或无权修正");
        }
        assertProductionLogStagePermission(log);
        LocalDateTime correctedTime = resolveRequiredBusinessTime(request.getCreateTime(), "流转记录时间");
        productionOrderStatusLogMapper.update(null, new LambdaUpdateWrapper<ProductionOrderStatusLog>()
                .eq(ProductionOrderStatusLog::getId, log.getId())
                .eq(ProductionOrderStatusLog::getTenantCode, tenantCode)
                .set(ProductionOrderStatusLog::getCreateTime, correctedTime));
        orderWarningCacheService.invalidate(tenantCode);
        OperationLogSkipContext.skipCurrent();
    }

    @Transactional(rollbackFor = Exception.class)
    public String createProductionOrder(ProductionOrderSaveRequest request) {
        ProductionOrder order = new ProductionOrder();
        order.setOrderId(codeGeneratorUtil.generateProductionOrderCode());
        order.setTenantCode(TenantPermissionContext.getTenantCode());
        order.setCreator(resolveCurrentUser());
        order.setUpdater(resolveCurrentUser());
        applyProductionOrderContent(order, request);
        assertProductionOrderStagePermission(order, order.getStatus());
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
        assertProductionOrderStagePermission(order, order.getStatus());
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
        updateProductionOrderInternal(orderId, request, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveProductionOrderTransition(String orderId, String targetStatus, String remark) {
        ProductionOrderUpdateRequest request = new ProductionOrderUpdateRequest();
        request.setStatus(targetStatus);
        request.setRemark(remark);
        updateProductionOrderInternal(orderId, request, true);
    }

    private void updateProductionOrderInternal(String orderId, ProductionOrderUpdateRequest request, boolean approvalBypass) {
        ProductionOrder order = findProductionOrder(orderId);
        String oldStatusText = buildProductionStateText(order.getStatus(), order.getProcess());
        String oldStatus = order.getStatus();
        Integer oldProcess = order.getProcess();
        String targetStatus = resolveProductionUpdateStatus(oldStatus, request.getStatus());
        if (!approvalBypass && STATUS_PENDING_PAY.equals(oldStatus) && STATUS_PENDING_MATERIAL.equals(targetStatus)) {
            throw new BusinessException("待收款订单转备料中需要先通过订单审批");
        }
        validateProductionStatusForward(oldStatus, targetStatus);
        if (!approvalBypass) {
            assertOrderStatusTransitionPermission(oldStatus, targetStatus);
            assertProductionOrderStagePermission(order, targetStatus);
        }
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

    @Transactional(rollbackFor = Exception.class)
    public void submitProductionOrderRollbackApproval(String orderId, ProductionOrderUpdateRequest request) {
        ProductionOrder order = findProductionOrder(orderId);
        String currentStatus = normalizeStatus(order.getStatus());
        String targetStatus = request != null && StringUtils.hasText(request.getStatus())
                ? request.getStatus().trim()
                : resolvePreviousProductionStatus(order);
        validateProductionRollbackTarget(currentStatus, targetStatus);
        assertOrderStatusActionPermission(currentStatus, "rollback");
        assertProductionOrderStagePermission(order, currentStatus);
        String approvalCode = orderApprovalCode(ORDER_TYPE_PRODUCTION, order.getOrderId());
        if (!approvalAuditorCandidateService.findPendingAuditorIds(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode).isEmpty()) {
            throw new BusinessException("该订单已有待处理审批，请审批完成后再操作");
        }
        List<Long> auditorIds = normalizeApprovalAuditorIds(request == null ? null : request.getAuditorIds());
        String requiredPermission = PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
        if (auditorIds.isEmpty()) {
            auditorIds = approvalDefaultAuditorService.resolveAuditorIds(
                    order.getTenantCode(),
                    APPROVAL_TYPE_ORDER,
                    TenantPermissionContext.getUserId(),
                    null,
                    null,
                    requiredPermission,
                    false);
        }
        List<Long> permittedIds = employeeMapper.selectActiveApproverIdsByPermission(
                order.getTenantCode(), requiredPermission);
        for (Long auditorId : auditorIds) {
            if (permittedIds == null || !permittedIds.contains(auditorId)) {
                throw new BusinessException("所选审批人没有订单审批权限");
            }
        }
        String remark = request == null ? null : blankToNull(request.getRemark());
        insertProductionRollbackLog(order, currentStatus, targetStatus, OrderLogOperateTypeEnum.ROLLBACK_PENDING.getCode(),
                StringUtils.hasText(remark)
                        ? remark
                        : "提交订单回退审批：" + orderStatusLabel(currentStatus) + " → " + orderStatusLabel(targetStatus));
        approvalAuditorCandidateService.replaceActiveCandidates(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode, auditorIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveProductionOrderRollback(String orderId, String remark) {
        ProductionOrder order = findProductionOrder(orderId);
        ProductionOrderStatusLog rollbackLog = findPendingProductionRollbackLog(order.getOrderId());
        if (rollbackLog == null) {
            throw new BusinessException("未找到待审批的订单回退申请");
        }
        String oldStatus = normalizeStatus(order.getStatus());
        String sourceStatus = normalizeStatus(rollbackLog.getOldStatus());
        String targetStatus = normalizeStatus(rollbackLog.getNewStatus());
        if (!Objects.equals(oldStatus, sourceStatus)) {
            throw new BusinessException("订单状态已变化，请重新提交回退审批");
        }
        validateProductionRollbackTarget(oldStatus, targetStatus);
        assertProductionOrderStagePermission(order, oldStatus);
        Integer oldProcess = order.getProcess();
        Integer targetProcess = resolveProductionRollbackProcess(oldStatus, targetStatus, oldProcess);

        LambdaUpdateWrapper<ProductionOrder> updateWrapper = new LambdaUpdateWrapper<ProductionOrder>()
                .eq(ProductionOrder::getOrderId, order.getOrderId())
                .eq(ProductionOrder::getStatus, oldStatus)
                .set(ProductionOrder::getStatus, targetStatus)
                .set(ProductionOrder::getProcess, targetProcess)
                .set(ProductionOrder::getUpdater, resolveCurrentUser())
                .set(ProductionOrder::getUpdateTime, LocalDateTime.now());
        if (oldProcess == null) {
            updateWrapper.isNull(ProductionOrder::getProcess);
        } else {
            updateWrapper.eq(ProductionOrder::getProcess, oldProcess);
        }
        int updatedRows = productionOrderMapper.update(null, updateWrapper);
        if (updatedRows == 0) {
            throw new BusinessException("订单状态已被其他操作更新，请刷新后重试");
        }
        order.setStatus(targetStatus);
        order.setProcess(targetProcess);
        order.setUpdater(resolveCurrentUser());
        order.setUpdateTime(LocalDateTime.now());
        orderWarningCacheService.invalidate(order.getTenantCode());
        insertProductionRollbackLog(order, oldStatus, targetStatus, OrderLogOperateTypeEnum.ROLLBACK_APPROVED.getCode(),
                StringUtils.hasText(remark) ? remark.trim() : "订单回退审批通过");
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
        order.setInformationChannel(resolveSalesInformationChannel(orderCategory, request.getInformationChannel()));
        order.setExpressCompany(blankToNull(request.getExpressCompany()));
        order.setExpressNo(blankToNull(request.getExpressNo()));
        order.setIsInvoice(normalizeInvoiceFlag(request.getIsInvoice()));
        order.setAttachmentName(blankToNull(request.getAttachmentName()));
        order.setAttachmentUrl(normalizeSalesOrderAttachmentUrlForStorage(request.getAttachmentUrl()));
        order.setAttachmentSize(request.getAttachmentSize());
        order.setStatus(resolveSalesStatusForCategory(orderCategory, request.getStatus(), order.getStatus(), createMode));
        validateShippingInfo(order.getStatus(), order.getExpressCompany(), order.getExpressNo());
        ensureCustomerProjectExists(order.getCustomerName(), order.getCustomerPhone(), order.getProjectName());
        order.setGoodsDesc(buildSalesGoodsDesc(request.getItems()));
        order.setTotalQuantity(sumSalesQuantity(request.getItems()));
    }

    private String resolveSalesInformationChannel(String orderCategory, String informationChannel) {
        if (isDrawingBudgetOrder(orderCategory)) {
            return blankToNull(informationChannel);
        }
        return requireText(informationChannel, "销售订单信息渠道不能为空");
    }

    private boolean canAutoCreateProductionOrder(String orderCategory) {
        String normalized = normalizeOrderCategory(orderCategory);
        return !CATEGORY_DRAWING_BUDGET.equals(normalized) && !CATEGORY_SPECIAL_ORDER.equals(normalized);
    }

    private Integer normalizeInvoiceFlag(Integer value) {
        return BinaryFlagEnum.isYes(value) ? BinaryFlagEnum.YES.getCode() : BinaryFlagEnum.NO.getCode();
    }

    private void replaceSalesOrderItems(String orderId, List<SalesOrderSaveRequest.ItemDTO> items, LocalDateTime businessCreateTime) {
        salesOrderDetailMapper.delete(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId));

        LocalDateTime now = LocalDateTime.now();
        List<SalesOrderDetail> details = normalizeSalesItems(items).stream().map(item -> {
            SalesOrderDetail detail = new SalesOrderDetail();
            detail.setOrderId(orderId);
            detail.setTenantCode(TenantPermissionContext.getTenantCode());
            detail.setModelCode(blankToNull(item.getModelCode()));
            detail.setWeight(item.getWeight());
            detail.setSpec(blankToNull(formatNumber(item.getSpec())));
            detail.setQuantity(item.getQuantity());
            detail.setCreateTime(businessCreateTime == null ? now : businessCreateTime);
            detail.setUpdateTime(now);
            return detail;
        }).toList();

        details.forEach(salesOrderDetailMapper::insert);
    }

    private void createProductionOrdersFromSales(SalesOrder order, List<SalesOrderSaveRequest.ItemDTO> items) {
        normalizeSalesItems(items).stream()
                .filter(item -> StringUtils.hasText(item.getModelCode()))
                .forEach(item -> {
            ProductionOrder productionOrder = new ProductionOrder();
            productionOrder.setTenantCode(TenantPermissionContext.getTenantCode());
            productionOrder.setOrderId(codeGeneratorUtil.generateProductionOrderCode());
            productionOrder.setSalesOrderId(order.getOrderId());
            productionOrder.setStatus(OrderStatusEnum.PENDING_CONFIRM.getCode());
            productionOrder.setModelCode(item.getModelCode().trim());
            productionOrder.setFabric(blankToNull(item.getWeight()));
            productionOrder.setWeight(null);
            productionOrder.setWidth(toBigDecimal(item.getSpec()));
            productionOrder.setQuantity(item.getQuantity() == null ? 0 : item.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue());
            productionOrder.setCustomerName(order.getCustomerName());
            productionOrder.setProjectName(order.getProjectName());
            productionOrder.setBrandName(order.getBrandName());
            productionOrder.setOrderCategory(order.getOrderCategory());
            productionOrder.setInformationChannel(order.getInformationChannel());
            productionOrder.setCreator(resolveCurrentUser());
            productionOrder.setUpdater(resolveCurrentUser());
            productionOrder.setCreateTime(order.getCreateTime());
            productionOrder.setUpdateTime(LocalDateTime.now());
            productionOrderMapper.insert(productionOrder);
        });
    }

    private void applyProductionOrderContent(ProductionOrder order, ProductionOrderSaveRequest request) {
        order.setSalesOrderId(blankToNull(request.getSalesOrderId()));
        order.setStatus(resolveProductionSaveStatus(order.getStatus(), request.getStatus()));
        order.setModelCode(request.getModelCode().trim());
        order.setFabric(blankToNull(request.getFabric()));
        order.setWeight(toBigDecimal(request.getWeight()));
        order.setWidth(toBigDecimal(request.getSpec()));
        order.setColor(blankToNull(request.getColor()));
        order.setQuantity(request.getQuantity());
        order.setCustomerName(blankToNull(request.getCustomerName()));
        order.setProjectName(blankToNull(request.getProjectName()));
        order.setBrandName(blankToNull(request.getBrandName()));
        order.setOrderCategory(OrderCategoryEnum.normalize(request.getOrderCategory()));
        order.setContactPhone(blankToNull(request.getContactPhone()));
        order.setInformationChannel(resolveProductionInformationChannel(
                order.getOrderCategory(), request.getInformationChannel()));
        order.setProcess(resolveProcess(order.getStatus(), request.getProcess()));
        ensureCustomerProjectExists(order.getCustomerName(), order.getContactPhone(), order.getProjectName());
    }

    private String resolveProductionInformationChannel(String orderCategory, String informationChannel) {
        if (isDrawingBudgetOrder(orderCategory)) {
            return blankToNull(informationChannel);
        }
        return requireText(informationChannel, "生产订单信息渠道不能为空");
    }

    private String resolveProductionSaveStatus(String currentStatus, String requestedStatus) {
        String status = StringUtils.hasText(requestedStatus)
                ? requestedStatus.trim()
                : defaultProductionStatus(currentStatus);
        if (!PRODUCTION_STATUS_CODES.contains(status)) {
            throw new BusinessException("生产订单状态不合法");
        }
        return status;
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

    private void insertProductionRollbackLog(ProductionOrder order, String oldStatus, String newStatus, String operateType, String remark) {
        ProductionOrderStatusLog log = new ProductionOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperateType(operateType);
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端提交订单回退审批");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(LocalDateTime.now());
        productionOrderStatusLogMapper.insert(log);
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String operateType, String remark) {
        insertSalesLog(order, oldStatus, operateType, remark, LocalDateTime.now());
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String operateType, String remark, LocalDateTime createTime) {
        insertSalesLog(order, oldStatus, order.getStatus(), operateType, remark, createTime);
    }

    private void insertSalesLog(SalesOrder order, String oldStatus, String newStatus, String operateType, String remark, LocalDateTime createTime) {
        SalesOrderStatusLog log = new SalesOrderStatusLog();
        log.setTenantCode(order.getTenantCode());
        log.setOrderId(order.getOrderId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperateType(operateType);
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理端更新销售订单信息");
        log.setOperator(resolveCurrentUser());
        log.setOperatorName(resolveCurrentUserName());
        log.setCreateTime(createTime == null ? LocalDateTime.now() : createTime);
        salesOrderStatusLogMapper.insert(log);
    }

    public SalesOrderStatusLog findPendingSalesRollbackLog(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return salesOrderStatusLogMapper.selectOne(new LambdaQueryWrapper<SalesOrderStatusLog>()
                .eq(SalesOrderStatusLog::getOrderId, orderId.trim())
                .eq(SalesOrderStatusLog::getOperateType, OrderLogOperateTypeEnum.ROLLBACK_PENDING.getCode())
                .orderByDesc(SalesOrderStatusLog::getId)
                .last("LIMIT 1"));
    }

    public boolean hasPendingSalesRollbackApproval(String orderId) {
        SalesOrder order = StringUtils.hasText(orderId) ? salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderId, orderId.trim())
                .last("LIMIT 1")) : null;
        if (order == null) {
            return false;
        }
        SalesOrderStatusLog log = findPendingSalesRollbackLog(order.getOrderId());
        if (log == null || !Objects.equals(normalizeStatus(order.getStatus()), normalizeStatus(log.getOldStatus()))) {
            return false;
        }
        return !approvalAuditorCandidateService.findPendingAuditorIds(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, orderApprovalCode(ORDER_TYPE_SALES, order.getOrderId())).isEmpty();
    }

    public ProductionOrderStatusLog findPendingProductionRollbackLog(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return productionOrderStatusLogMapper.selectOne(new LambdaQueryWrapper<ProductionOrderStatusLog>()
                .eq(ProductionOrderStatusLog::getOrderId, orderId.trim())
                .eq(ProductionOrderStatusLog::getOperateType, OrderLogOperateTypeEnum.ROLLBACK_PENDING.getCode())
                .orderByDesc(ProductionOrderStatusLog::getId)
                .last("LIMIT 1"));
    }

    public boolean hasPendingProductionRollbackApproval(String orderId) {
        ProductionOrder order = StringUtils.hasText(orderId) ? productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getOrderId, orderId.trim())
                .last("LIMIT 1")) : null;
        if (order == null) {
            return false;
        }
        ProductionOrderStatusLog log = findPendingProductionRollbackLog(order.getOrderId());
        if (log == null || !Objects.equals(normalizeStatus(order.getStatus()), normalizeStatus(log.getOldStatus()))) {
            return false;
        }
        return !approvalAuditorCandidateService.findPendingAuditorIds(
                order.getTenantCode(), APPROVAL_TYPE_ORDER, orderApprovalCode(ORDER_TYPE_PRODUCTION, order.getOrderId())).isEmpty();
    }

    private String resolveStatusBeforePendingCancel(String orderId) {
        SalesOrderStatusLog latestCancelLog = salesOrderStatusLogMapper.selectOne(new LambdaQueryWrapper<SalesOrderStatusLog>()
                .eq(SalesOrderStatusLog::getOrderId, orderId)
                .eq(SalesOrderStatusLog::getNewStatus, STATUS_PENDING_CANCEL)
                .orderByDesc(SalesOrderStatusLog::getId)
                .last("LIMIT 1"));
        if (latestCancelLog == null || !StringUtils.hasText(latestCancelLog.getOldStatus())) {
            throw new BusinessException("缺少取消申请来源状态，无法驳回取消申请");
        }
        String restoredStatus = latestCancelLog.getOldStatus().trim();
        if (STATUS_PENDING_CANCEL.equals(restoredStatus) || STATUS_CANCELLED.equals(restoredStatus)) {
            throw new BusinessException("取消申请来源状态异常，无法驳回取消申请");
        }
        return restoredStatus;
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

    private void assertDirectSalesTransitionAllowed(String orderCategory, String oldStatus, String targetStatus) {
        if (CATEGORY_SPECIAL_ORDER.equals(normalizeOrderCategory(orderCategory))
                && STATUS_PENDING_CONFIRM.equals(oldStatus)
                && STATUS_PENDING_PAY.equals(targetStatus)) {
            throw new BusinessException("特殊订单需要先通过订单审核，审核通过后才能创建成功");
        }
        if (STATUS_PENDING_PAY.equals(oldStatus) && STATUS_PENDING_MATERIAL.equals(targetStatus)) {
            throw new BusinessException("待收款订单转备料中需要先通过订单审批");
        }
        if (STATUS_PENDING_SHIP.equals(oldStatus) && STATUS_SHIPPED.equals(targetStatus)) {
            throw new BusinessException("待发货订单转已发货需要先通过订单审批");
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
        syncInstallationTaskIfCompleted(linkedSalesOrder);
    }

    private void syncInstallationTaskIfCompleted(SalesOrder order) {
        if (order != null && OrderStatusEnum.COMPLETED.matches(order.getStatus())) {
            installationTaskService.createOrSyncFromCompletedOrder(order);
        }
    }

    private boolean supportsProductionStatusSync(String status) {
        return STATUS_PENDING_MATERIAL.equals(status) || OrderStatusEnum.supportsSalesProductionSync(status);
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
        payload.put("informationChannel", safePrintText(order.getInformationChannel(), ""));
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
        payload.put("barcode", flowScanCode);
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
        Map<String, Object> qrPayload = new LinkedHashMap<>();
        qrPayload.put("version", "1");
        qrPayload.put("codeType", "order_flow");
        qrPayload.put("orderType", stringValue(payload.get("orderType")));
        qrPayload.put("orderId", stringValue(payload.get("orderId")));
        qrPayload.put("flowCode", stringValue(payload.get("flowCode")));
        qrPayload.put("flowScanCode", stringValue(payload.get("flowScanCode")));
        qrPayload.put("generatedAt", stringValue(payload.get("generatedAt")));
        return toSimpleJson(qrPayload);
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
            case "pending_cancel" -> "取消审核中";
            case "cancelled" -> "已取消";
            default -> "扫码识别";
        };
    }

    private String orderCategoryLabel(String category) {
        if (CATEGORY_SPECIAL_ORDER.equals(normalizeOrderCategory(category))) {
            return "特殊订单";
        }
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
            throw new BusinessException("订单状态不合法");
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
            throw new BusinessException("订单状态不合法");
        }
        if (targetIndex <= currentIndex) {
            throw new BusinessException("订单状态不能回退");
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

    private void assertSalesLogStagePermission(SalesOrderStatusLog log) {
        String status = StringUtils.hasText(log.getNewStatus()) ? log.getNewStatus() : log.getOldStatus();
        if (!StringUtils.hasText(status)) {
            SalesOrder order = findSalesOrder(log.getOrderId());
            status = order.getStatus();
        }
        assertSalesOrderStagePermission(null, status);
    }

    private void assertProductionLogStagePermission(ProductionOrderStatusLog log) {
        ProductionOrder order = findProductionOrder(log.getOrderId());
        String status = StringUtils.hasText(log.getNewStatus()) ? log.getNewStatus() : order.getStatus();
        assertProductionOrderStagePermission(order, status);
    }

    private void assertSalesOrderStagePermission(SalesOrder order, String targetStatus) {
        if (hasOrderStatusAccess(targetStatus)) {
            return;
        }
        throw new BusinessException(403, "当前账号没有权限维护" + orderStatusStageName(targetStatus)
                + "，请联系管理员在角色管理中分配对应订单权限");
    }

    private String orderStatusPermission(String status) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().replace('_', '-') : "";
        return PermissionCatalogV3.CODE_ORDER_STATUS_PREFIX + normalizedStatus + ":view";
    }

    @Transactional(rollbackFor = Exception.class)
    public void advanceSalesOrderByFlowCode(String rawFlowCode) {
        final OrderFlowCodeUtil.Parsed parsed;
        try {
            parsed = OrderFlowCodeUtil.parse(rawFlowCode);
        } catch (RuntimeException ex) {
            throw new BusinessException("订单流转码无效，请重新打印后再扫码");
        }
        if (!ORDER_TYPE_SALES.equals(parsed.orderType())) {
            throw new BusinessException("请扫描销售订单流转码");
        }
        if (!OrderFlowCodeUtil.matches(orderFlowCodeSecret, TenantPermissionContext.getTenantCode(), parsed)) {
            throw new BusinessException("订单流转码无效，请重新打印后再扫码");
        }
        advanceSalesOrderToNextStage(parsed.orderId(), new SalesOrderUpdateRequest());
    }

    private void assertOrderStatusTransitionPermission(String currentStatus, String targetStatus) {
        String current = normalizeStatus(currentStatus);
        String target = normalizeStatus(targetStatus);
        if (Objects.equals(current, target)) {
            return;
        }
        if (STATUS_PENDING_CANCEL.equals(target)) {
            assertOrderStatusActionPermission(current, "cancel");
            return;
        }
        int currentIndex = SALES_STATUS_CODES.indexOf(current);
        int targetIndex = SALES_STATUS_CODES.indexOf(target);
        String action = currentIndex >= 0 && targetIndex >= 0 && targetIndex < currentIndex
                ? "rollback"
                : "advance";
        assertOrderStatusActionPermission(current, action);
    }

    private void assertOrderStatusActionPermission(String status, String action) {
        String normalizedStatus = normalizeStatus(status).replace('_', '-');
        String permissionCode = PermissionCatalogV3.CODE_ORDER_STATUS_PREFIX + normalizedStatus + ":" + action;
        if (!TenantPermissionContext.hasPermission(permissionCode)) {
            throw new BusinessException(403, "当前账号没有对应的订单状态操作权限");
        }
    }

    private String orderAuditPermission(SalesOrder order) {
        String status = order == null ? "" : normalizeStatus(order.getStatus());
        if (STATUS_PENDING_CANCEL.equals(status)) {
            return PermissionCatalogV3.CODE_ORDER_AUDIT_CANCEL;
        }
        if (STATUS_PENDING_PAY.equals(status)) {
            return PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL;
        }
        return PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
    }

    private String orderStatusStageName(String status) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : "";
        return StringUtils.hasText(normalizedStatus) ? "状态为“" + normalizedStatus + "”的订单" : "该状态订单";
    }

    private void assertProductionOrderStagePermission(ProductionOrder order, String targetStatus) {
        if (hasOrderStatusAccess(targetStatus)) {
            return;
        }
        throw new BusinessException(403, "当前账号没有权限维护" + orderStatusStageName(targetStatus)
                + "，请联系管理员在角色管理中分配对应订单权限");
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
                .eq(SalesOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("sales order not found");
        }
        return order;
    }

    private SalesOrder findSalesOrderForUpdate(String orderId) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        SalesOrder order = salesOrderMapper.selectByOrderIdForUpdate(tenantCode, orderId);
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

    private Map<String, List<SalesOrderDetail>> buildSalesDetailMap(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> orderIds = orders.stream().map(SalesOrder::getOrderId).toList();
        return salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                        .in(SalesOrderDetail::getOrderId, orderIds)
                        .orderByAsc(SalesOrderDetail::getId))
                .stream()
                .collect(Collectors.groupingBy(SalesOrderDetail::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<ProductionOrder>> buildFulfillmentMap(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> orderIds = orders.stream()
                .map(SalesOrder::getOrderId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                        .eq(ProductionOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                        .in(ProductionOrder::getSalesOrderId, orderIds)
                        .orderByAsc(ProductionOrder::getId))
                .stream()
                .filter(order -> StringUtils.hasText(order.getSalesOrderId()))
                .collect(Collectors.groupingBy(ProductionOrder::getSalesOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void applyFulfillmentView(SalesOrderPageVO vo,
                                      List<ProductionOrder> fulfillmentRecords,
                                      String canonicalStatus) {
        FulfillmentSnapshot snapshot = fulfillmentSnapshot(fulfillmentRecords, canonicalStatus);
        vo.setFulfillmentTracked(snapshot.tracked());
        vo.setFulfillmentRecordCount(snapshot.recordCount());
        vo.setProcess(snapshot.process());
        vo.setProcessText(snapshot.view().processText());
        vo.setCurrentProcessText(snapshot.view().currentProcessText());
        vo.setCompletedProcessText(snapshot.view().completedProcessText());
        vo.setProcessProgressPercent(snapshot.view().progressPercent());
        vo.setProcessSteps(snapshot.view().steps());
    }

    private void applyFulfillmentView(SalesOrderDetailVO vo,
                                      List<ProductionOrder> fulfillmentRecords,
                                      String canonicalStatus) {
        FulfillmentSnapshot snapshot = fulfillmentSnapshot(fulfillmentRecords, canonicalStatus);
        vo.setFulfillmentTracked(snapshot.tracked());
        vo.setFulfillmentRecordCount(snapshot.recordCount());
        vo.setProcess(snapshot.process());
        vo.setProcessText(snapshot.view().processText());
        vo.setCurrentProcessText(snapshot.view().currentProcessText());
        vo.setCompletedProcessText(snapshot.view().completedProcessText());
        vo.setProcessProgressPercent(snapshot.view().progressPercent());
        vo.setProcessSteps(snapshot.view().steps());
    }

    private FulfillmentSnapshot fulfillmentSnapshot(List<ProductionOrder> fulfillmentRecords,
                                                     String canonicalStatus) {
        List<ProductionOrder> records = fulfillmentRecords == null ? Collections.emptyList() : fulfillmentRecords;
        Integer process = records.stream().anyMatch(order -> order.getProcess() == null)
                ? null
                : records.stream().map(ProductionOrder::getProcess).min(Integer::compareTo).orElse(null);
        return new FulfillmentSnapshot(
                !records.isEmpty(),
                records.size(),
                process,
                buildProductionProcessView(canonicalStatus, process)
        );
    }

    private SalesOrderPageVO.ItemVO toSalesOrderPageItem(SalesOrderDetail detail) {
        SalesOrderPageVO.ItemVO vo = new SalesOrderPageVO.ItemVO();
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }

    private String buildSalesGoodsDesc(List<SalesOrderSaveRequest.ItemDTO> items) {
        List<SalesOrderSaveRequest.ItemDTO> normalizedItems = normalizeSalesItems(items);
        if (normalizedItems.isEmpty()) {
            return null;
        }
        return normalizedItems.stream()
                .map(item -> {
                    String category = blankToNull(item.getWeight());
                    String spec = formatNumber(item.getSpec());
                    return (blankToNull(item.getModelCode()) == null ? "未填写型号" : item.getModelCode().trim())
                            + " / " + (StringUtils.hasText(category) ? category : "未填写类别")
                            + " / " + (StringUtils.hasText(spec) ? spec + "规格" : "未填写规格")
                            + " × " + optionalText(formatQuantity(item.getQuantity()), "未填写数量");
                })
                .collect(Collectors.joining("；"));
    }

    private Integer sumSalesQuantity(List<SalesOrderSaveRequest.ItemDTO> items) {
        BigDecimal total = normalizeSalesItems(items).stream()
                .map(SalesOrderSaveRequest.ItemDTO::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.intValue();
    }

    private List<SalesOrderSaveRequest.ItemDTO> normalizeSalesItems(List<SalesOrderSaveRequest.ItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(this::hasSalesItemContent)
                .toList();
    }

    private boolean hasSalesItemContent(SalesOrderSaveRequest.ItemDTO item) {
        return StringUtils.hasText(item.getModelCode())
                || item.getQuantity() != null
                || StringUtils.hasText(item.getWeight())
                || item.getSpec() != null;
    }

    private String optionalText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatQuantity(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
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

    private String resolveSalesStatusForCategory(String orderCategory, String requestedStatus, String currentStatus, boolean createMode) {
        String category = normalizeOrderCategory(orderCategory);
        String requested = StringUtils.hasText(requestedStatus) ? requestedStatus.trim() : "";
        if (isDrawingBudgetOrder(category)) {
            if (!StringUtils.hasText(requested) || STATUS_PENDING_CONFIRM.equals(requested)) {
                return isBudgetStatus(currentStatus) ? currentStatus.trim() : STATUS_BUDGETING;
            }
            if (createMode && STATUS_BUDGET_COMPLETED.equals(requested)) {
                throw new BusinessException("图纸预算订单创建时只能处于预算中状态");
            }
            if (!isBudgetStatus(requested)) {
                throw new BusinessException("图纸预算订单只能使用预算中或预算完成状态");
            }
            return requested;
        }
        if (isBudgetStatus(requested)) {
            throw new BusinessException("只有图纸预算订单可以使用预算状态");
        }
        if (createMode && CATEGORY_SPECIAL_ORDER.equals(category)) {
            return STATUS_PENDING_CONFIRM;
        }
        if (STATUS_CANCELLED.equals(requested)) {
            return STATUS_PENDING_CANCEL;
        }
        if (StringUtils.hasText(requested)
                && !SALES_FORWARD_STATUS_CODES.contains(requested)
                && !STATUS_PENDING_CANCEL.equals(requested)) {
            throw new BusinessException("销售订单状态不合法");
        }
        return StringUtils.hasText(requested) ? requested : defaultSalesStatus(currentStatus);
    }

    private void validateSalesStatusForward(String orderCategory, String currentStatus, String targetStatus) {
        String target = StringUtils.hasText(targetStatus) ? targetStatus.trim() : defaultSalesStatus(currentStatus);
        String current = StringUtils.hasText(currentStatus) ? currentStatus.trim() : defaultSalesStatus(null);
        if (isDrawingBudgetOrder(orderCategory)
                && (STATUS_PENDING_CANCEL.equals(current) || STATUS_CANCELLED.equals(current)
                || STATUS_PENDING_CANCEL.equals(target) || STATUS_CANCELLED.equals(target)
                || (STATUS_BUDGET_COMPLETED.equals(current) && !STATUS_BUDGET_COMPLETED.equals(target)))) {
            throw new BusinessException("图纸预算订单只能从预算中流转到预算完成，预算完成后不能取消或回退");
        }
        if (Objects.equals(current, target)) {
            return;
        }
        if (STATUS_PENDING_CANCEL.equals(target)) {
            if (STATUS_CANCELLED.equals(current)) {
                throw new BusinessException("已取消的订单不能重复提交取消审核");
            }
            return;
        }
        if (STATUS_CANCELLED.equals(target)) {
            if (!STATUS_PENDING_CANCEL.equals(current)) {
                throw new BusinessException("取消订单需要先通过订单审核");
            }
            return;
        }
        List<String> flow = isDrawingBudgetOrder(orderCategory) ? SALES_BUDGET_FORWARD_STATUS_CODES : SALES_FORWARD_STATUS_CODES;
        int currentIndex = flow.indexOf(current);
        int targetIndex = flow.indexOf(target);
        if (currentIndex < 0 || targetIndex < 0) {
            throw new BusinessException("销售订单状态不合法");
        }
        if (targetIndex <= currentIndex) {
            throw new BusinessException("销售订单状态不能回退");
        }
        if (targetIndex > currentIndex + 1) {
            throw new BusinessException("销售订单状态不能跳级流转");
        }
    }

    private String resolveNextSalesStatus(SalesOrder order) {
        String category = order == null ? CATEGORY_BULK : normalizeOrderCategory(order.getOrderCategory());
        String current = order == null ? "" : String.valueOf(order.getStatus()).trim();
        List<String> flow = isDrawingBudgetOrder(category) ? SALES_BUDGET_FORWARD_STATUS_CODES : SALES_FORWARD_STATUS_CODES;
        if (!StringUtils.hasText(current)) {
            return flow.get(0);
        }
        int currentIndex = flow.indexOf(current);
        if (currentIndex < 0) {
            throw new BusinessException("销售订单当前状态不支持自动推进");
        }
        if (currentIndex >= flow.size() - 1) {
            throw new BusinessException("销售订单已经处于最终阶段");
        }
        return flow.get(currentIndex + 1);
    }

    private String resolvePreviousSalesStatus(SalesOrder order) {
        String category = order == null ? CATEGORY_BULK : normalizeOrderCategory(order.getOrderCategory());
        String current = order == null ? "" : normalizeStatus(order.getStatus());
        if (isDrawingBudgetOrder(category) && STATUS_BUDGET_COMPLETED.equals(current)) {
            throw new BusinessException("图纸预算订单预算完成后不能提交回退审批");
        }
        if (STATUS_PENDING_CANCEL.equals(current) || STATUS_CANCELLED.equals(current)) {
            throw new BusinessException("取消中或已取消订单不能提交回退审批");
        }
        List<String> flow = isDrawingBudgetOrder(category) ? SALES_BUDGET_FORWARD_STATUS_CODES : SALES_FORWARD_STATUS_CODES;
        int currentIndex = flow.indexOf(current);
        if (currentIndex <= 0) {
            throw new BusinessException("当前订单没有可回退的上一状态");
        }
        return flow.get(currentIndex - 1);
    }

    private String resolvePreviousProductionStatus(ProductionOrder order) {
        String current = order == null ? "" : normalizeStatus(order.getStatus());
        int currentIndex = PRODUCTION_STATUS_CODES.indexOf(current);
        if (currentIndex <= 0) {
            throw new BusinessException("当前订单没有可回退的上一状态");
        }
        return PRODUCTION_STATUS_CODES.get(currentIndex - 1);
    }

    private void validateProductionRollbackTarget(String currentStatus, String targetStatus) {
        String current = normalizeStatus(currentStatus);
        String target = normalizeStatus(targetStatus);
        if (!StringUtils.hasText(current) || !StringUtils.hasText(target)) {
            throw new BusinessException("订单回退状态不能为空");
        }
        int currentIndex = PRODUCTION_STATUS_CODES.indexOf(current);
        int targetIndex = PRODUCTION_STATUS_CODES.indexOf(target);
        if (currentIndex < 0 || targetIndex < 0) {
            throw new BusinessException("订单回退状态不合法");
        }
        if (targetIndex != currentIndex - 1) {
            throw new BusinessException("订单只能回退到上一状态");
        }
    }

    private Integer resolveProductionRollbackProcess(String currentStatus, String targetStatus, Integer currentProcess) {
        if (STATUS_PRODUCING.equals(currentStatus) && !STATUS_PRODUCING.equals(targetStatus)) {
            return null;
        }
        if (STATUS_PRODUCING.equals(targetStatus) && currentProcess == null) {
            return PRODUCTION_PROCESS_LABELS.size() - 1;
        }
        return currentProcess;
    }

    private void validateSalesRollbackTarget(SalesOrder order, String currentStatus, String targetStatus) {
        String category = order == null ? CATEGORY_BULK : normalizeOrderCategory(order.getOrderCategory());
        String current = normalizeStatus(currentStatus);
        String target = normalizeStatus(targetStatus);
        if (!StringUtils.hasText(current) || !StringUtils.hasText(target)) {
            throw new BusinessException("订单回退状态不能为空");
        }
        if (STATUS_PENDING_CANCEL.equals(current) || STATUS_CANCELLED.equals(current)
                || STATUS_PENDING_CANCEL.equals(target) || STATUS_CANCELLED.equals(target)) {
            throw new BusinessException("取消相关状态不能作为订单回退目标");
        }
        if (isDrawingBudgetOrder(category) && STATUS_BUDGET_COMPLETED.equals(current)) {
            throw new BusinessException("图纸预算订单预算完成后不能审批回退");
        }
        List<String> flow = isDrawingBudgetOrder(category) ? SALES_BUDGET_FORWARD_STATUS_CODES : SALES_FORWARD_STATUS_CODES;
        int currentIndex = flow.indexOf(current);
        int targetIndex = flow.indexOf(target);
        if (currentIndex < 0 || targetIndex < 0) {
            throw new BusinessException("订单回退状态不合法");
        }
        if (targetIndex != currentIndex - 1) {
            throw new BusinessException("订单只能回退到上一状态");
        }
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "";
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

    private record FulfillmentSnapshot(
            boolean tracked,
            int recordCount,
            Integer process,
            ProductionProcessView view
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

    private LambdaQueryWrapper<SalesOrder> scopedSalesOrderWrapper(Set<String> permittedStatuses) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        applyOrderStatusPermissionFilter(wrapper, SalesOrder::getStatus, permittedStatuses);
        return wrapper;
    }

    private LambdaQueryWrapper<ProductionOrder> scopedProductionOrderWrapper(Set<String> permittedStatuses) {
        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<>();
        applyOrderStatusPermissionFilter(wrapper, ProductionOrder::getStatus, permittedStatuses);
        return wrapper;
    }

    private <T> void applyOrderStatusPermissionFilter(LambdaQueryWrapper<T> wrapper,
                                                       SFunction<T, String> statusColumn,
                                                       Set<String> permittedStatuses) {
        if (permittedStatuses == null) {
            return;
        }
        if (permittedStatuses.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(statusColumn, permittedStatuses);
    }

    private Set<String> permittedOrderStatuses(List<String> supportedStatuses) {
        Set<String> supported = new LinkedHashSet<>(supportedStatuses);
        Set<String> permittedStatuses = new LinkedHashSet<>();
        for (String status : supported) {
            if (hasOrderStatusAccess(status)) {
                permittedStatuses.add(status);
            }
        }
        return permittedStatuses.size() == supported.size() ? null : permittedStatuses;
    }

    private boolean hasOrderStatusAccess(String status) {
        Set<String> permCodes = TenantPermissionContext.getPermCodes();
        String requiredPermission = orderStatusPermission(status);
        if (matchesPermission(permCodes, requiredPermission, "!")) {
            return false;
        }
        return matchesPermission(permCodes, requiredPermission, "");
    }

    private boolean matchesPermission(Set<String> permCodes, String permCode, String prefix) {
        if (permCodes == null || permCodes.isEmpty() || !StringUtils.hasText(permCode)) {
            return false;
        }
        String normalizedPermCode = permCode.trim();
        return permCodes.contains(prefix + normalizedPermCode);
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
                || !Objects.equals(before.getTotalQuantity(), after.getTotalQuantity())
                || !sameText(before.getInformationChannel(), after.getInformationChannel())
                || !sameText(before.getExpressCompany(), after.getExpressCompany())
                || !sameText(before.getExpressNo(), after.getExpressNo())
                || !Objects.equals(before.getIsInvoice(), after.getIsInvoice())
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
                || !Objects.equals(before.getProcess(), after.getProcess())
                || !sameText(before.getCustomerName(), after.getCustomerName())
                || !sameText(before.getProjectName(), after.getProjectName())
                || !sameText(before.getBrandName(), after.getBrandName())
                || !sameText(before.getContactPhone(), after.getContactPhone())
                || !sameText(before.getInformationChannel(), after.getInformationChannel());
    }

    private boolean salesOrderItemsChanged(String orderId, List<SalesOrderSaveRequest.ItemDTO> items) {
        List<SalesOrderDetail> existingItems = salesOrderDetailMapper.selectList(new LambdaQueryWrapper<SalesOrderDetail>()
                .eq(SalesOrderDetail::getOrderId, orderId)
                .orderByAsc(SalesOrderDetail::getId));
        List<SalesOrderSaveRequest.ItemDTO> requestItems = normalizeSalesItems(items);
        if (existingItems.size() != requestItems.size()) {
            return true;
        }
        for (int i = 0; i < existingItems.size(); i++) {
            SalesOrderDetail existing = existingItems.get(i);
            SalesOrderSaveRequest.ItemDTO request = requestItems.get(i);
            if (!sameText(existing.getModelCode(), request.getModelCode())
                    || !sameText(existing.getWeight(), request.getWeight())
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
        wrapper.notIn(SalesOrder::getStatus, OrderStatusEnum.COMPLETED.getCode(), STATUS_PENDING_CANCEL, OrderStatusEnum.CANCELLED.getCode(), STATUS_BUDGET_COMPLETED)
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
                && !OrderStatusEnum.PENDING_CANCEL.matches(status)
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

    private LocalDateTime resolveRequiredBusinessTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        try {
            LocalDateTime parsedTime = parseDateTime(value);
            if (parsedTime == null) {
                throw new BusinessException(fieldName + "不能为空");
            }
            if (parsedTime.isAfter(LocalDateTime.now().plusMinutes(1))) {
                throw new BusinessException(fieldName + "不能晚于当前时间");
            }
            return parsedTime;
        } catch (BusinessException ex) {
            throw ex;
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
