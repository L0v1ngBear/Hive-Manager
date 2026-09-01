package my.hive.domain.aftersales.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.domain.aftersales.mapper.AfterSalesPartMapper;
import my.hive.domain.aftersales.mapper.AfterSalesPartStockRecordMapper;
import my.hive.domain.aftersales.mapper.AfterSalesTicketMapper;
import my.hive.domain.aftersales.mapper.AfterSalesTicketPartMapper;
import my.hive.domain.aftersales.model.dto.AfterSalesPartSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesPartStockInRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketPageRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketAssignRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketFollowUpRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketStatusRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesRepairImageRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesPart;
import my.hive.domain.aftersales.model.entity.AfterSalesPartStockRecord;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.domain.aftersales.model.entity.AfterSalesTicketPart;
import my.hive.domain.aftersales.model.vo.AfterSalesRepairImageVO;
import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.vo.CustomerOptionVO;
import my.hive.domain.customer.service.CustomerService;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.domain.order.model.vo.SalesOrderShipmentVO;
import my.hive.domain.order.service.OrderLogisticsTrackingService;
import my.hive.domain.order.service.OrderShipmentService;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.employee.model.vo.EmployeeLeaderOptionVO;
import my.hive.infrastructure.wechat.WechatSubscribeService;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.PageResult;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.security.InternalUploadUrlValidator;
import my.hive.shared.utils.ExcelUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AfterSalesService {
    private static final int MAX_TICKET_EXPORT_ROWS = 10_000;
    private static final int MAX_REPAIR_IMAGES = 9;
    private static final long MAX_REPAIR_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> TICKET_TYPES = Set.of("consultation", "diagnosis", "resend_parts", "on_site_repair", "motor_replacement");
    private static final Set<String> PRIORITIES = Set.of("low", "normal", "high", "urgent");
    private static final Set<String> PART_LOCATIONS = Set.of("三车间", "二车间", "其他");
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_WAITING_OUTBOUND = "waiting_outbound";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_PENDING_APPROVAL = "pending_approval";
    private static final String APPROVAL_TYPE_AFTER_SALES = "after_sales";

    @Resource private AfterSalesTicketMapper ticketMapper;
    @Resource private AfterSalesPartMapper partMapper;
    @Resource private AfterSalesTicketPartMapper ticketPartMapper;
    @Resource private AfterSalesPartStockRecordMapper stockRecordMapper;
    @Resource private SalesOrderMapper salesOrderMapper;
    @Resource private CustomerMapper customerMapper;
    @Resource private CustomerContactMapper customerContactMapper;
    @Resource private CustomerService customerService;
    @Resource private OrderShipmentService orderShipmentService;
    @Resource private OrderLogisticsTrackingService orderLogisticsTrackingService;
    @Resource private EmployeeMapper employeeMapper;
    @Resource private WechatSubscribeService wechatSubscribeService;
    @Resource private ApprovalDefaultAuditorService approvalDefaultAuditorService;
    @Resource private ApprovalAuditorCandidateService approvalAuditorCandidateService;
    @Resource private ExcelUtil excelUtil;
    @Value("${server.servlet.context-path:}") private String contextPath;

    public PageResult<AfterSalesTicket> ticketPage(AfterSalesTicketPageRequest request) {
        AfterSalesTicketPageRequest safe = request == null ? new AfterSalesTicketPageRequest() : request;
        LambdaQueryWrapper<AfterSalesTicket> wrapper = buildTicketQuery(safe);
        wrapper.orderByDesc(AfterSalesTicket::getUpdateTime).orderByDesc(AfterSalesTicket::getId);
        Page<AfterSalesTicket> page = ticketMapper.selectPage(new Page<>(safePage(safe.getPageNum()), safeSize(safe.getPageSize())), wrapper);
        return toPageResult(page);
    }

    public void exportTickets(AfterSalesTicketPageRequest request, HttpServletResponse response) {
        AfterSalesTicketPageRequest safe = request == null ? new AfterSalesTicketPageRequest() : request;
        LambdaQueryWrapper<AfterSalesTicket> wrapper = buildTicketQuery(safe).orderByDesc(AfterSalesTicket::getCreateTime).orderByDesc(AfterSalesTicket::getId);
        long total = ticketMapper.selectCount(wrapper);
        if (total > MAX_TICKET_EXPORT_ROWS) throw new BusinessException("导出数据超过 " + MAX_TICKET_EXPORT_ROWS + " 行，请缩小时间筛选范围后重试");
        List<String> headers = List.of("工单号", "创建时间", "项目名称", "客户名称", "关联订单", "处理方式", "负责人", "状态", "紧急程度", "物流公司", "物流单号", "联系人", "联系电话", "开业时间", "实际地址", "服务地址", "维修金额", "旧电机退还数量", "问题描述", "故障研判", "处理结果");
        List<List<String>> rows = ticketMapper.selectList(wrapper).stream().map(ticket -> List.of(
                excelUtil.stringify(ticket.getTicketNo()), excelUtil.stringify(ticket.getCreateTime()),
                excelUtil.stringify(ticket.getProjectName()), excelUtil.stringify(ticket.getCustomerName()), excelUtil.stringify(ticket.getOrderId()),
                ticketTypeLabel(ticket.getTicketType()), excelUtil.stringify(ticket.getAssigneeName()), statusLabel(ticket.getStatus()), priorityLabel(ticket.getPriority()),
                excelUtil.stringify(ticket.getLogisticsCompany()), excelUtil.stringify(ticket.getWaybillNo()), excelUtil.stringify(ticket.getContactName()),
                excelUtil.stringify(ticket.getContactPhone()), excelUtil.stringify(ticket.getOpeningDate()), excelUtil.stringify(ticket.getActualAddress()),
                excelUtil.stringify(ticket.getServiceAddress()), excelUtil.stringify(ticket.getRepairAmount()), excelUtil.stringify(ticket.getReturnOldMotorQuantity()),
                excelUtil.stringify(ticket.getProblemDesc()), excelUtil.stringify(ticket.getDiagnosis()), excelUtil.stringify(ticket.getResolution())
        )).toList();
        excelUtil.writeRowsToResponse(response, "售后工单", headers, rows, "售后工单.xlsx");
    }

    private LambdaQueryWrapper<AfterSalesTicket> buildTicketQuery(AfterSalesTicketPageRequest request) {
        LambdaQueryWrapper<AfterSalesTicket> wrapper = new LambdaQueryWrapper<AfterSalesTicket>()
                .eq(AfterSalesTicket::getTenantCode, TenantPermissionContext.getTenantCode());
        String keyword = clean(request.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(AfterSalesTicket::getTicketNo, keyword)
                    .or().like(AfterSalesTicket::getProjectName, keyword)
                    .or().like(AfterSalesTicket::getCustomerName, keyword)
                    .or().like(AfterSalesTicket::getOrderId, keyword));
        }
        if (clean(request.getStatus()) != null) wrapper.eq(AfterSalesTicket::getStatus, request.getStatus().trim());
        if (clean(request.getTicketType()) != null) wrapper.eq(AfterSalesTicket::getTicketType, request.getTicketType().trim());
        LocalDate startDate = request.getCreatedStartDate();
        LocalDate endDate = request.getCreatedEndDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) throw new BusinessException("开始日期不能晚于结束日期");
        if (startDate != null) wrapper.ge(AfterSalesTicket::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(AfterSalesTicket::getCreateTime, endDate.plusDays(1).atStartOfDay());
        return wrapper;
    }

    private String ticketTypeLabel(String value) { return switch (value == null ? "" : value) { case "consultation" -> "咨询"; case "diagnosis" -> "故障研判"; case "resend_parts" -> "补发配件"; case "on_site_repair" -> "上门维修"; case "motor_replacement" -> "更换电机"; default -> value == null ? "" : value; }; }
    private String statusLabel(String value) { return switch (value == null ? "" : value) { case "draft" -> "草稿"; case "pending_approval" -> "待售后审核"; case "waiting_outbound" -> "待配件出库"; case "processing" -> "处理中"; case "closed" -> "已结案"; case "cancelled" -> "已取消"; default -> value == null ? "" : value; }; }
    private String priorityLabel(String value) { return switch (value == null ? "" : value) { case "low" -> "低"; case "normal" -> "普通"; case "high" -> "高"; case "urgent" -> "紧急"; default -> value == null ? "" : value; }; }

    public AfterSalesTicket ticketDetail(Long id) {
        AfterSalesTicket ticket = requireTicket(id);
        ticket.setParts(ticketPartMapper.selectList(new LambdaQueryWrapper<AfterSalesTicketPart>()
                .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode())
                .eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                .orderByAsc(AfterSalesTicketPart::getId)));
        ticket.setRepairImages(resolveRepairImages(ticket.getAttachmentUrlsJson()));
        return ticket;
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTicket saveTicket(AfterSalesTicketSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String type = requireType(request.getTicketType());
        String requestedOrderId = clean(request.getOrderId());
        SalesOrder order = requestedOrderId == null ? null : salesOrderMapper.selectByOrderIdForUpdate(tenantCode, requestedOrderId);
        if (requestedOrderId != null && order == null) throw new BusinessException("关联订单不存在或不属于当前组织");
        String customerName = order == null ? cleanRequired(request.getCustomerName(), "未关联订单时请填写客户名称") : order.getCustomerName();
        String customerPhone = order == null ? clean(request.getContactPhone()) : order.getCustomerPhone();
        String projectName = order == null ? clean(request.getProjectName()) : order.getProjectName();
        if (order == null) {
            Customer customer = customerService.ensureAfterSalesCustomer(
                    customerName, request.getContactName(), request.getContactPhone(), projectName);
            customerName = customer.getCustomerName();
        }
        AfterSalesTicket ticket;
        boolean creating = request.getId() == null;
        if (creating) {
            ticket = new AfterSalesTicket();
            ticket.setTenantCode(tenantCode);
            ticket.setTicketNo(nextTicketNo());
            ticket.setCreatorUserId(TenantPermissionContext.getUserId());
            ticket.setCreatorName(currentOperatorName());
        } else {
            ticket = requireTicket(request.getId());
            if (!STATUS_DRAFT.equals(ticket.getStatus())) throw new BusinessException("仅草稿工单可以编辑，已提交工单请通过处理操作推进");
        }
        ticket.setOrderId(order == null ? null : order.getOrderId());
        ticket.setCustomerName(customerName);
        ticket.setCustomerPhone(customerPhone);
        ticket.setProjectName(projectName);
        ticket.setContactName(clean(request.getContactName()));
        ticket.setContactPhone(clean(request.getContactPhone()));
        ticket.setServiceAddress(clean(request.getServiceAddress()));
        ticket.setActualAddress(clean(request.getActualAddress()));
        ticket.setOpeningDate(request.getOpeningDate());
        ticket.setTicketType(type);
        ticket.setPriority(normalizePriority(request.getPriority()));
        ticket.setProblemDesc(cleanRequired(request.getProblemDesc(), "请填写问题描述"));
        ticket.setDiagnosis(clean(request.getDiagnosis()));
        ticket.setResolution(clean(request.getResolution()));
        ticket.setScheduledTime(request.getScheduledTime());
        ticket.setTechnicianName(clean(request.getTechnicianName()));
        ticket.setWaybillNo(clean(request.getWaybillNo()));
        ticket.setLogisticsCompany(clean(request.getLogisticsCompany()));
        ticket.setOldMotorInfo(clean(request.getOldMotorInfo()));
        ticket.setReturnOldMotor(Boolean.TRUE.equals(request.getReturnOldMotor()) ? 1 : 0);
        ticket.setReturnOldMotorQuantity(normalizeNonNegativeInteger(request.getReturnOldMotorQuantity(), "旧电机退还数量不能小于 0"));
        ticket.setRepairAmount(normalizeNonNegativeAmount(request.getRepairAmount(), "维修金额不能小于 0"));
        if (creating || request.getRepairImages() != null || request.getAttachmentUrlsJson() != null) {
            List<AfterSalesRepairImageVO> repairImages = normalizeRepairImages(resolveRequestedRepairImages(request));
            ticket.setAttachmentUrlsJson(repairImages.isEmpty() ? null : JSON.toJSONString(repairImages));
        }
        ticket.setApprovalRequired(Boolean.TRUE.equals(request.getApprovalRequired()) ? 1 : 0);
        ticket.setStatus(Boolean.TRUE.equals(request.getApprovalRequired()) ? STATUS_PENDING_APPROVAL : (hasParts(request.getParts()) ? STATUS_WAITING_OUTBOUND : STATUS_DRAFT));
        if (creating) ticketMapper.insert(ticket); else ticketMapper.updateById(ticket);
        replaceTicketParts(ticket, request.getParts());
        if (Boolean.TRUE.equals(request.getApprovalRequired())) submitTicketApproval(ticket);
        return ticketDetail(ticket.getId());
    }

    private List<AfterSalesRepairImageRequest> resolveRequestedRepairImages(AfterSalesTicketSaveRequest request) {
        if (request.getRepairImages() != null) {
            return request.getRepairImages();
        }
        String legacyJson = clean(request.getAttachmentUrlsJson());
        if (legacyJson == null) {
            return List.of();
        }
        try {
            List<AfterSalesRepairImageRequest> images = JSON.parseArray(legacyJson, AfterSalesRepairImageRequest.class);
            return images == null ? List.of() : images;
        } catch (RuntimeException exception) {
            throw new BusinessException("维修图片信息格式不正确");
        }
    }

    private List<AfterSalesRepairImageVO> normalizeRepairImages(List<AfterSalesRepairImageRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        if (requested.size() > MAX_REPAIR_IMAGES) {
            throw new BusinessException("维修图片最多上传9张");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        Map<String, AfterSalesRepairImageVO> uniqueImages = new LinkedHashMap<>();
        for (AfterSalesRepairImageRequest image : requested) {
            if (image == null) {
                throw new BusinessException("维修图片信息不能为空");
            }
            String fileName = cleanRequired(image.getFileName(), "维修图片名称不能为空");
            if (fileName.length() > 255) {
                throw new BusinessException("维修图片名称不能超过255个字符");
            }
            String fileUrl = InternalUploadUrlValidator.normalizeStoredUploadUrl(
                    cleanRequired(image.getFileUrl(), "维修图片地址不能为空"),
                    contextPath,
                    tenantCode,
                    "after-sales-repair"
            );
            Long fileSize = image.getFileSize();
            if (fileSize != null && (fileSize < 0 || fileSize > MAX_REPAIR_IMAGE_BYTES)) {
                throw new BusinessException("单张维修图片不能超过5MB");
            }
            if (uniqueImages.containsKey(fileUrl)) {
                throw new BusinessException("同一维修图片不能重复添加");
            }
            AfterSalesRepairImageVO normalized = new AfterSalesRepairImageVO();
            normalized.setFileName(fileName);
            normalized.setFileUrl(fileUrl);
            normalized.setFileSize(fileSize);
            uniqueImages.put(fileUrl, normalized);
        }
        return new ArrayList<>(uniqueImages.values());
    }

    private List<AfterSalesRepairImageVO> resolveRepairImages(String attachmentUrlsJson) {
        if (StringUtils.isBlank(attachmentUrlsJson)) {
            return List.of();
        }
        try {
            List<AfterSalesRepairImageVO> images = JSON.parseArray(attachmentUrlsJson, AfterSalesRepairImageVO.class);
            if (images == null) {
                return List.of();
            }
            return images.stream()
                    .filter(image -> image != null && !StringUtils.isBlank(image.getFileUrl()))
                    .limit(MAX_REPAIR_IMAGES)
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    /**
     * Legacy tickets only recorded a waybill.  If that waybill is also on the
     * linked order, resolve its carrier automatically; new tickets retain their
     * own carrier so their after-sales dispatch can be tracked independently.
     */
    public OrderLogisticsTrackingVO ticketLogisticsTracking(Long ticketId) {
        AfterSalesTicket ticket = requireTicket(ticketId);
        String waybill = clean(ticket.getWaybillNo());
        if (waybill == null) {
            throw new BusinessException("该售后工单尚未填写物流单号");
        }
        String company = clean(ticket.getLogisticsCompany());
        if (company == null) {
            SalesOrderShipmentVO orderShipment = orderShipmentService
                    .listShipments(ticket.getTenantCode(), ticket.getOrderId())
                    .stream()
                    .filter(item -> waybill.equals(clean(item.getTrackingNo())))
                    .findFirst()
                    .orElse(null);
            if (orderShipment != null) {
                return orderLogisticsTrackingService.getTracking(ticket.getOrderId(), orderShipment.getId());
            }
            throw new BusinessException("该售后工单缺少物流公司，请编辑工单后补充");
        }
        return orderLogisticsTrackingService.getTrackingForAfterSales(
                ticket.getOrderId(), company, waybill, ticket.getId());
    }

    /**
     * Assignees are active members of the current tenant.  Assignment does not
     * grant permissions; access still follows each member's existing role.
     */
    public List<EmployeeLeaderOptionVO> assigneeOptions(String keyword) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String safeKeyword = clean(keyword);
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, tenantCode)
                .eq(Employee::getStatus, 1);
        if (safeKeyword != null) {
            wrapper.and(item -> item.like(Employee::getName, safeKeyword)
                    .or().like(Employee::getDepartmentName, safeKeyword)
                    .or().like(Employee::getPosition, safeKeyword));
        }
        wrapper.orderByAsc(Employee::getName).orderByAsc(Employee::getId).last("LIMIT 50");
        return employeeMapper.selectList(wrapper).stream().map(employee -> {
            EmployeeLeaderOptionVO option = new EmployeeLeaderOptionVO();
            option.setId(employee.getId());
            option.setName(employee.getName());
            option.setDepartmentName(employee.getDepartmentName());
            option.setPositionName(employee.getPosition());
            return option;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTicket assignTicket(Long ticketId, AfterSalesTicketAssignRequest request) {
        AfterSalesTicket ticket = requireTicket(ticketId);
        requireTicketStatus(ticket, Set.of(STATUS_DRAFT, STATUS_WAITING_OUTBOUND, STATUS_PROCESSING), "已结案或已取消的工单不能再指派");
        Employee assignee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, ticket.getTenantCode())
                .eq(Employee::getId, request.getAssigneeUserId())
                .eq(Employee::getStatus, 1)
                .last("LIMIT 1"));
        if (assignee == null) {
            throw new BusinessException("指派人员不存在、已停用或不属于当前组织");
        }
        if (assignee.getId().equals(ticket.getAssigneeUserId())) {
            return ticketDetail(ticket.getId());
        }
        ticket.setAssigneeUserId(assignee.getId());
        ticket.setAssigneeName(assignee.getName());
        ticketMapper.updateById(ticket);
        wechatSubscribeService.sendTodoAfterCommit(assignee.getId(), "售后工单已指派",
                ticket.getTicketNo() + " · " + ticket.getProjectName(), "pages/index/index");
        return ticketDetail(ticket.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTicketStatus(AfterSalesTicketStatusRequest request) {
        AfterSalesTicket ticket = requireTicket(request.getTicketId());
        String action = cleanRequired(request.getAction(), "操作不能为空");
        if ("start".equals(action)) {
            requireTicketStatus(ticket, Set.of(STATUS_DRAFT), "只有草稿工单可以开始处理");
            long pending = ticketPartMapper.selectCount(new LambdaQueryWrapper<AfterSalesTicketPart>()
                    .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                    .eq(AfterSalesTicketPart::getLineStatus, "pending"));
            if (pending > 0) throw new BusinessException("工单仍有待出库配件，请先完成出库");
            updateTicketStatus(ticket, STATUS_PROCESSING, null);
            return;
        }
        if ("close".equals(action)) {
            requireTicketStatus(ticket, Set.of(STATUS_PROCESSING), "只有处理中的工单可以结案");
            String resolution = clean(request.getResolution());
            if (resolution == null && clean(ticket.getResolution()) == null) throw new BusinessException("结案前请填写处理结果");
            updateTicketStatus(ticket, STATUS_CLOSED, resolution);
            return;
        }
        if ("cancel".equals(action)) {
            requireTicketStatus(ticket, Set.of(STATUS_DRAFT, STATUS_WAITING_OUTBOUND), "当前工单状态不允许取消");
            long outbound = ticketPartMapper.selectCount(new LambdaQueryWrapper<AfterSalesTicketPart>()
                    .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                    .eq(AfterSalesTicketPart::getLineStatus, "outbound"));
            if (outbound > 0) throw new BusinessException("已有配件出库，不能直接取消，请先处理退回");
            updateTicketStatus(ticket, STATUS_CANCELLED, null);
            ticketPartMapper.update(null, new LambdaUpdateWrapper<AfterSalesTicketPart>()
                    .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                    .eq(AfterSalesTicketPart::getLineStatus, "pending").set(AfterSalesTicketPart::getLineStatus, "cancelled"));
            return;
        }
        throw new BusinessException("不支持的售后工单操作");
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTicket followUpTicket(Long ticketId, AfterSalesTicketFollowUpRequest request) {
        AfterSalesTicket ticket = requireTicket(ticketId);
        requireTicketStatus(ticket, Set.of(STATUS_CLOSED), "只有已结案工单可以回访");
        ticket.setFollowUpTime(request.getFollowUpTime() == null ? LocalDateTime.now() : request.getFollowUpTime());
        ticket.setFollowUpOperatorName(currentOperatorName());
        ticket.setFollowUpSatisfaction(normalizeFollowUpSatisfaction(request.getSatisfaction()));
        ticket.setFollowUpResolved(Boolean.TRUE.equals(request.getResolved()) ? 1 : 0);
        ticket.setFollowUpContent(cleanRequired(request.getContent(), "请填写回访内容"));
        ticketMapper.updateById(ticket);
        return ticketDetail(ticket.getId());
    }

    private void submitTicketApproval(AfterSalesTicket ticket) {
        List<Long> auditorIds = approvalDefaultAuditorService.resolveAuditorIds(ticket.getTenantCode(),
                ApprovalDefaultAuditorService.TYPE_AFTER_SALES, ticket.getCreatorUserId(), null, null,
                PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, false);
        ticket.setApprovalStatus("pending");
        ticket.setApprovalAuditorId(auditorIds.get(0));
        ticket.setApprovalAuditorIds(auditorIds.size() > 1 ? String.join(",", auditorIds.stream().map(String::valueOf).toList()) : null);
        ticketMapper.updateById(ticket);
        approvalAuditorCandidateService.replaceActiveCandidates(ticket.getTenantCode(), APPROVAL_TYPE_AFTER_SALES,
                ticket.getTicketNo(), auditorIds, approvalDefaultAuditorService.resolveApprovalMode(ticket.getTenantCode(), ApprovalDefaultAuditorService.TYPE_AFTER_SALES));
    }

    public List<AfterSalesTicket> approvalTickets() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        return ticketMapper.selectList(new LambdaQueryWrapper<AfterSalesTicket>()
                        .eq(AfterSalesTicket::getTenantCode, tenantCode)
                        .eq(AfterSalesTicket::getApprovalRequired, 1)
                        .orderByDesc(AfterSalesTicket::getUpdateTime))
                .stream().filter(ticket -> userId != null && (userId.equals(ticket.getCreatorUserId())
                        || approvalAuditorCandidateService.findRelatedApprovalCodes(tenantCode, APPROVAL_TYPE_AFTER_SALES, userId).contains(ticket.getTicketNo())))
                .peek(ticket -> ticket.setCanAudit(userId != null && approvalAuditorCandidateService.isPendingAuditor(tenantCode,
                        APPROVAL_TYPE_AFTER_SALES, ticket.getTicketNo(), userId)))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTicket auditTicketApproval(Long ticketId, Boolean approved, String comment) {
        AfterSalesTicket ticket = requireTicket(ticketId);
        requireTicketStatus(ticket, Set.of(STATUS_PENDING_APPROVAL), "当前工单不处于待审核状态");
        var decision = approvalAuditorCandidateService.recordDecision(ticket.getTenantCode(), APPROVAL_TYPE_AFTER_SALES,
                ticket.getTicketNo(), TenantPermissionContext.getUserId(), Boolean.TRUE.equals(approved), clean(comment));
        if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) return ticketDetail(ticket.getId());
        ticket.setApprovalStatus(decision == ApprovalAuditorCandidateService.ApprovalDecision.APPROVED ? "approved" : "rejected");
        ticket.setStatus(hasPendingParts(ticket) ? STATUS_WAITING_OUTBOUND : STATUS_DRAFT);
        ticketMapper.updateById(ticket);
        approvalAuditorCandidateService.closeActiveCandidates(ticket.getTenantCode(), APPROVAL_TYPE_AFTER_SALES, ticket.getTicketNo());
        return ticketDetail(ticket.getId());
    }

    private boolean hasPendingParts(AfterSalesTicket ticket) {
        return ticketPartMapper.selectCount(new LambdaQueryWrapper<AfterSalesTicketPart>()
                .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode())
                .eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                .eq(AfterSalesTicketPart::getLineStatus, "pending")) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long ticketId) {
        AfterSalesTicket ticket = requireTicket(ticketId);
        requireTicketStatus(ticket, Set.of(STATUS_WAITING_OUTBOUND), "只有待配件出库的工单可以出库");
        List<AfterSalesTicketPart> lines = ticketPartMapper.selectList(new LambdaQueryWrapper<AfterSalesTicketPart>()
                .eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesTicketPart::getTicketId, ticket.getId())
                .eq(AfterSalesTicketPart::getLineStatus, "pending").orderByAsc(AfterSalesTicketPart::getId));
        if (lines.isEmpty()) throw new BusinessException("没有待出库配件");
        for (AfterSalesTicketPart line : lines) {
            AfterSalesPart part = requirePart(line.getPartId(), true);
            int before = safeQty(part.getStockQty());
            int quantity = safePositive(line.getQuantity(), "配件出库数量必须大于0");
            int updated = partMapper.update(null, new LambdaUpdateWrapper<AfterSalesPart>()
                    .eq(AfterSalesPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesPart::getId, part.getId())
                    .eq(AfterSalesPart::getVersion, part.getVersion()).ge(AfterSalesPart::getStockQty, quantity)
                    .set(AfterSalesPart::getStockQty, before - quantity).set(AfterSalesPart::getVersion, part.getVersion() + 1));
            if (updated != 1) throw new BusinessException("配件“" + part.getPartName() + "”库存不足或已被其他出库操作更新");
            line.setLineStatus("outbound");
            ticketPartMapper.updateById(line);
            stockRecordMapper.insert(stockRecord(ticket, part, "out", quantity, before, before - quantity, line.getRemark()));
        }
        updateTicketStatus(ticket, STATUS_PROCESSING, null);
    }

    public PageResult<AfterSalesPart> partPage(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<AfterSalesPart> wrapper = new LambdaQueryWrapper<AfterSalesPart>()
                .eq(AfterSalesPart::getTenantCode, TenantPermissionContext.getTenantCode());
        String safeKeyword = clean(keyword);
        if (safeKeyword != null) wrapper.and(w -> w.like(AfterSalesPart::getPartCode, safeKeyword).or().like(AfterSalesPart::getPartName, safeKeyword).or().like(AfterSalesPart::getModelSpec, safeKeyword));
        wrapper.orderByDesc(AfterSalesPart::getUpdateTime).orderByDesc(AfterSalesPart::getId);
        return toPageResult(partMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesPart savePart(AfterSalesPartSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        boolean creating = request.getId() == null;
        AfterSalesPart part = creating ? new AfterSalesPart() : requirePart(request.getId(), false);
        String code = clean(request.getPartCode());
        if (creating) {
            part.setTenantCode(tenantCode);
            part.setPartCode(code == null ? nextPartCode() : code);
            part.setStockQty(0); part.setReservedQty(0); part.setVersion(0);
        } else if (code != null && !code.equals(part.getPartCode())) {
            throw new BusinessException("配件编码创建后不可修改");
        }
        Long duplicate = partMapper.selectCount(new LambdaQueryWrapper<AfterSalesPart>().eq(AfterSalesPart::getTenantCode, tenantCode)
                .eq(AfterSalesPart::getPartCode, part.getPartCode()).ne(!creating, AfterSalesPart::getId, part.getId()));
        if (duplicate != null && duplicate > 0) throw new BusinessException("配件编码已存在");
        part.setPartName(cleanRequired(request.getPartName(), "请填写配件名称"));
        if (creating || request.getCategory() != null) part.setCategory(clean(request.getCategory()));
        part.setModelSpec(clean(request.getModelSpec()));
        part.setUnitPrice(request.getUnitPrice());
        part.setUnit(clean(request.getUnit()) == null ? "个" : clean(request.getUnit()));
        if (creating || request.getSafetyStock() != null) part.setSafetyStock(request.getSafetyStock() == null ? 0 : request.getSafetyStock());
        part.setPhotoUrl(clean(request.getPhotoUrl())); part.setRemark(clean(request.getRemark()));
        part.setStatus(normalizePartStatus(request.getStatus()));
        if (creating) partMapper.insert(part); else partMapper.updateById(part);
        return part;
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockIn(AfterSalesPartStockInRequest request) {
        AfterSalesPart part = requirePart(request.getPartId(), false);
        int quantity = safePositive(request.getQuantity(), "入库数量必须大于0");
        int before = safeQty(part.getStockQty());
        int updated = partMapper.update(null, new LambdaUpdateWrapper<AfterSalesPart>()
                .eq(AfterSalesPart::getTenantCode, part.getTenantCode()).eq(AfterSalesPart::getId, part.getId()).eq(AfterSalesPart::getVersion, part.getVersion())
                .set(AfterSalesPart::getStockQty, before + quantity).set(AfterSalesPart::getVersion, part.getVersion() + 1));
        if (updated != 1) throw new BusinessException("配件库存已被其他操作更新，请刷新后重试入库");
        stockRecordMapper.insert(stockRecord(null, part, "in", quantity, before, before + quantity, clean(request.getRemark())));
    }

    public List<SalesOrder> orderOptions(String keyword) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getTenantCode, tenantCode);
        String safeKeyword = clean(keyword);
        if (safeKeyword != null) wrapper.and(w -> w.like(SalesOrder::getOrderId, safeKeyword).or().like(SalesOrder::getProjectName, safeKeyword).or().like(SalesOrder::getCustomerName, safeKeyword));
        wrapper.orderByDesc(SalesOrder::getUpdateTime).last("LIMIT 30");
        List<SalesOrder> orders = salesOrderMapper.selectList(wrapper);
        enrichOrderContacts(tenantCode, orders);
        return orders;
    }

    public List<CustomerOptionVO> customerOptions(String keyword) {
        return customerService.listCustomerOptions(keyword);
    }

    private void enrichOrderContacts(String tenantCode, List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        Set<String> customerNames = new LinkedHashSet<>();
        for (SalesOrder order : orders) if (clean(order.getCustomerName()) != null) customerNames.add(order.getCustomerName());
        if (customerNames.isEmpty()) return;
        Map<String, Long> customerIdsByName = new HashMap<>();
        for (Customer customer : customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode).in(Customer::getCustomerName, customerNames))) {
            customerIdsByName.put(customer.getCustomerName(), customer.getId());
        }
        if (customerIdsByName.isEmpty()) return;
        Map<Long, CustomerContact> contactsByCustomerId = new HashMap<>();
        for (CustomerContact contact : customerContactMapper.selectList(new LambdaQueryWrapper<CustomerContact>()
                .eq(CustomerContact::getTenantCode, tenantCode).in(CustomerContact::getCustomerId, customerIdsByName.values()).orderByAsc(CustomerContact::getId))) {
            contactsByCustomerId.putIfAbsent(contact.getCustomerId(), contact);
        }
        for (SalesOrder order : orders) {
            CustomerContact contact = contactsByCustomerId.get(customerIdsByName.get(order.getCustomerName()));
            if (contact != null) {
                order.setContactName(contact.getContactName());
                order.setContactPhone(contact.getContactPhone());
            }
        }
    }

    private void replaceTicketParts(AfterSalesTicket ticket, List<AfterSalesTicketSaveRequest.AfterSalesTicketPartItem> items) {
        ticketPartMapper.delete(new LambdaQueryWrapper<AfterSalesTicketPart>().eq(AfterSalesTicketPart::getTenantCode, ticket.getTenantCode()).eq(AfterSalesTicketPart::getTicketId, ticket.getId()));
        if (!hasParts(items)) return;
        Set<Long> seen = new LinkedHashSet<>();
        for (AfterSalesTicketSaveRequest.AfterSalesTicketPartItem item : items) {
            if (item == null) throw new BusinessException("配件明细不能为空");
            if (!seen.add(item.getPartId())) throw new BusinessException("同一配件请合并为一行");
            AfterSalesPart part = requirePart(item.getPartId(), true);
            AfterSalesTicketPart line = new AfterSalesTicketPart();
            line.setTenantCode(ticket.getTenantCode()); line.setTicketId(ticket.getId()); line.setPartId(part.getId());
            line.setPartCode(part.getPartCode()); line.setPartName(part.getPartName()); line.setModelSpec(part.getModelSpec()); line.setUnit(part.getUnit());
            line.setQuantity(safePositive(item.getQuantity(), "配件数量必须大于0")); line.setPartLocation(normalizePartLocation(item.getPartLocation())); line.setLineStatus("pending"); line.setRemark(clean(item.getRemark()));
            ticketPartMapper.insert(line);
        }
    }

    private AfterSalesTicket requireTicket(Long id) {
        if (id == null || id <= 0) throw new BusinessException("售后工单不能为空");
        AfterSalesTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<AfterSalesTicket>().eq(AfterSalesTicket::getTenantCode, TenantPermissionContext.getTenantCode()).eq(AfterSalesTicket::getId, id).last("LIMIT 1"));
        if (ticket == null) throw new BusinessException("售后工单不存在或不属于当前组织");
        return ticket;
    }
    private AfterSalesPart requirePart(Long id, boolean enabled) {
        if (id == null || id <= 0) throw new BusinessException("配件不能为空");
        LambdaQueryWrapper<AfterSalesPart> wrapper = new LambdaQueryWrapper<AfterSalesPart>().eq(AfterSalesPart::getTenantCode, TenantPermissionContext.getTenantCode()).eq(AfterSalesPart::getId, id).last("LIMIT 1");
        if (enabled) wrapper.eq(AfterSalesPart::getStatus, 1);
        AfterSalesPart part = partMapper.selectOne(wrapper);
        if (part == null) throw new BusinessException(enabled ? "配件不存在或已停用" : "配件不存在");
        return part;
    }
    private void updateTicketStatus(AfterSalesTicket ticket, String status, String resolution) {
        ticket.setStatus(status); if (resolution != null) ticket.setResolution(resolution); ticketMapper.updateById(ticket);
    }
    private void requireTicketStatus(AfterSalesTicket ticket, Set<String> allowedStatuses, String message) {
        if (!allowedStatuses.contains(ticket.getStatus())) throw new BusinessException(message);
    }
    private AfterSalesPartStockRecord stockRecord(AfterSalesTicket ticket, AfterSalesPart part, String type, int quantity, int before, int after, String remark) {
        AfterSalesPartStockRecord record = new AfterSalesPartStockRecord();
        record.setTenantCode(part.getTenantCode()); record.setPartId(part.getId()); record.setTicketId(ticket == null ? null : ticket.getId()); record.setOperateType(type);
        record.setQuantity(quantity); record.setBeforeQty(before); record.setAfterQty(after); record.setOperatorUserId(TenantPermissionContext.getUserId()); record.setOperatorName(currentOperatorName()); record.setRemark(remark);
        return record;
    }
    private String requireType(String type) { String safe = cleanRequired(type, "请选择处理方式"); if (!TICKET_TYPES.contains(safe)) throw new BusinessException("处理方式不合法"); return safe; }
    private String normalizePriority(String priority) { String safe = clean(priority); if (safe == null) return "normal"; if (!PRIORITIES.contains(safe)) throw new BusinessException("紧急程度不合法"); return safe; }
    private String normalizeFollowUpSatisfaction(String satisfaction) { String safe = cleanRequired(satisfaction, "请选择客户满意度"); if (!Set.of("满意", "一般", "不满意").contains(safe)) throw new BusinessException("客户满意度不合法"); return safe; }
    private String normalizePartLocation(String location) { String safe = clean(location); if (safe == null) return null; if (!PART_LOCATIONS.contains(safe)) throw new BusinessException("配件地点不合法"); return safe; }
    private Integer normalizeNonNegativeInteger(Integer value, String message) { if (value == null) return 0; if (value < 0) throw new BusinessException(message); return value; }
    private BigDecimal normalizeNonNegativeAmount(BigDecimal value, String message) { if (value == null) return null; if (value.signum() < 0) throw new BusinessException(message); return value; }
    private int normalizePartStatus(Integer status) { if (status == null) return 1; if (status != 0 && status != 1) throw new BusinessException("配件状态不合法"); return status; }
    private boolean hasParts(List<?> parts) { return parts != null && !parts.isEmpty(); }
    private int safePositive(Integer value, String message) { if (value == null || value <= 0) throw new BusinessException(message); return value; }
    private int safeQty(Integer value) { return value == null ? 0 : value; }
    private String cleanRequired(String value, String message) { String safe = clean(value); if (safe == null) throw new BusinessException(message); return safe; }
    private String clean(String value) { return StringUtils.isBlank(value) ? null : value.trim(); }
    private long safePage(Integer value) { return value == null || value <= 0 ? 1L : value; }
    private long safeSize(Integer value) { return value == null || value <= 0 ? 10L : Math.min(value, 100); }
    private <T> PageResult<T> toPageResult(Page<T> page) { PageResult<T> result = new PageResult<>(); result.setCurrent(page.getCurrent()); result.setSize(page.getSize()); result.setTotal(page.getTotal()); result.setPages(page.getPages()); result.setData(page.getRecords()); return result; }
    private String nextTicketNo() { return "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(); }
    private String nextPartCode() { return "PT" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(); }
    private String currentOperatorName() { Long userId = TenantPermissionContext.getUserId(); return userId == null ? "系统用户" : "用户" + userId; }
}
