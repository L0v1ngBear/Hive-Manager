package my.hive.api.aftersales;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.domain.aftersales.model.dto.AfterSalesPartSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesPartStockInRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketPageRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketAssignRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketApprovalAuditRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketFollowUpRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketStatusRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesPart;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.domain.aftersales.service.AfterSalesService;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.employee.model.vo.EmployeeLeaderOptionVO;
import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.infrastructure.storage.BusinessAttachmentService;
import my.hive.infrastructure.storage.BusinessAttachmentVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/after-sales")
@RequireTenantFeature(TenantFeatureEnum.CODE_AFTER_SALES)
public class AfterSalesController {
    private static final long PART_PHOTO_MAX_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> PART_PHOTO_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    @Resource private AfterSalesService afterSalesService;
    @Resource private BusinessAttachmentService businessAttachmentService;

    @GetMapping("/tickets")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_LIST, message = "当前账号没有查看售后工单权限")
    public Result<PageResult<AfterSalesTicket>> tickets(@Valid AfterSalesTicketPageRequest request) { return Result.success(afterSalesService.ticketPage(request)); }

    @GetMapping("/tickets/export")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_LIST, message = "当前账号没有导出售后工单权限")
    @CollectLog(module = "after_sales", action = "export_ticket", bizType = "after_sales_ticket", description = "导出售后工单")
    public void exportTickets(@Valid AfterSalesTicketPageRequest request, HttpServletResponse response) {
        afterSalesService.exportTickets(request, response);
    }

    @GetMapping("/tickets/{id}")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_DETAIL, message = "当前账号没有查看售后工单详情权限")
    public Result<AfterSalesTicket> ticketDetail(@PathVariable Long id) { return Result.success(afterSalesService.ticketDetail(id)); }

    @GetMapping("/tickets/{id}/logistics-tracking")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_DETAIL, message = "当前账号没有查看售后工单详情权限")
    public Result<OrderLogisticsTrackingVO> ticketLogisticsTracking(@PathVariable Long id) {
        return Result.success(afterSalesService.ticketLogisticsTracking(id));
    }

    @GetMapping("/assignee-options")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有指派售后工单权限")
    public Result<List<EmployeeLeaderOptionVO>> assigneeOptions(String keyword) {
        return Result.success(afterSalesService.assigneeOptions(keyword));
    }

    @PostMapping("/tickets")
    @CollectLog(module = "after_sales", action = "save_ticket", bizType = "after_sales_ticket", bizNo = "#request.orderId", description = "保存售后工单")
    public Result<AfterSalesTicket> saveTicket(@Valid @RequestBody AfterSalesTicketSaveRequest request) {
        requireSavePermission(request.getId() == null, "当前账号没有保存售后工单权限");
        return Result.success(afterSalesService.saveTicket(request));
    }

    @PostMapping("/tickets/status")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有处理售后工单权限")
    @CollectLog(module = "after_sales", action = "ticket_status", bizType = "after_sales_ticket", bizNo = "#request.ticketId", description = "推进售后工单")
    public Result<Void> updateStatus(@Valid @RequestBody AfterSalesTicketStatusRequest request) { afterSalesService.updateTicketStatus(request); return Result.success(null); }

    @PostMapping("/tickets/{id}/follow-up")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有回访售后工单权限")
    @CollectLog(module = "after_sales", action = "follow_up_ticket", bizType = "after_sales_ticket", bizNo = "#id", description = "回访售后工单")
    public Result<AfterSalesTicket> followUpTicket(@PathVariable Long id, @Valid @RequestBody AfterSalesTicketFollowUpRequest request) {
        return Result.success(afterSalesService.followUpTicket(id, request));
    }

    @PostMapping("/tickets/{id}/assignee")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有指派售后工单权限")
    @CollectLog(module = "after_sales", action = "assign_ticket", bizType = "after_sales_ticket", bizNo = "#id", description = "指派售后工单")
    public Result<AfterSalesTicket> assignTicket(@PathVariable Long id, @Valid @RequestBody AfterSalesTicketAssignRequest request) {
        return Result.success(afterSalesService.assignTicket(id, request));
    }

    @GetMapping("/approvals")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有查看售后审批权限")
    public Result<List<AfterSalesTicket>> approvals() { return Result.success(afterSalesService.approvalTickets()); }

    @PostMapping("/tickets/{id}/approval")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, message = "当前账号没有审批售后工单权限")
    @CollectLog(module = "after_sales", action = "audit_ticket", bizType = "after_sales_ticket", bizNo = "#id", description = "审批售后工单")
    public Result<AfterSalesTicket> auditTicket(@PathVariable Long id, @Valid @RequestBody AfterSalesTicketApprovalAuditRequest request) {
        return Result.success(afterSalesService.auditTicketApproval(id, request.getApproved(), request.getComment()));
    }

    @PostMapping("/tickets/{id}/outbound")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PART_OUTBOUND, message = "当前账号没有售后配件出库权限")
    @CollectLog(module = "after_sales", action = "part_outbound", bizType = "after_sales_ticket", bizNo = "#id", description = "售后配件出库")
    public Result<Void> outbound(@PathVariable Long id) { afterSalesService.outbound(id); return Result.success(null); }

    @GetMapping("/parts")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PART_LIST, message = "当前账号没有查看配件库权限")
    public Result<PageResult<AfterSalesPart>> parts(Integer pageNum, Integer pageSize, String keyword) { return Result.success(afterSalesService.partPage(pageNum, pageSize, keyword)); }

    @PostMapping("/parts")
    @CollectLog(module = "after_sales", action = "save_part", bizType = "after_sales_part", bizNo = "#request.partCode", description = "保存售后配件")
    public Result<AfterSalesPart> savePart(@Valid @RequestBody AfterSalesPartSaveRequest request) {
        requireSavePartPermission(request.getId() == null);
        return Result.success(afterSalesService.savePart(request));
    }

    @PostMapping(value = "/parts/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = {PermissionCatalogV3.CODE_AFTER_SALES_PART_CREATE, PermissionCatalogV3.CODE_AFTER_SALES_PART_UPDATE}, message = "当前账号没有上传配件图片权限")
    public Result<BusinessAttachmentVO> uploadPartPhoto(@RequestParam("file") MultipartFile file) {
        validatePartPhoto(file);
        return Result.success(businessAttachmentService.upload(file, "after-sales-part"));
    }

    @PostMapping("/parts/stock-in")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_PART_STOCK_IN, message = "当前账号没有配件入库权限")
    @CollectLog(module = "after_sales", action = "part_stock_in", bizType = "after_sales_part", bizNo = "#request.partId", description = "售后配件入库")
    public Result<Void> stockIn(@Valid @RequestBody AfterSalesPartStockInRequest request) { afterSalesService.stockIn(request); return Result.success(null); }

    @GetMapping("/order-options")
    @RequirePermission(value = PermissionCatalogV3.CODE_AFTER_SALES_CREATE, message = "当前账号没有新建售后工单权限")
    public Result<List<SalesOrder>> orderOptions(String keyword) { return Result.success(afterSalesService.orderOptions(keyword)); }

    private void validatePartPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择配件图片");
        if (file.getSize() > PART_PHOTO_MAX_SIZE) throw new BusinessException("配件图片不能超过5MB");
        String name = file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!PART_PHOTO_EXTENSIONS.contains(extension)) throw new BusinessException("配件图片仅支持 PNG、JPG、JPEG 或 WebP 格式");
    }

    private void requireSavePermission(boolean creating, String message) {
        String permission = creating ? PermissionCatalogV3.CODE_AFTER_SALES_CREATE : PermissionCatalogV3.CODE_AFTER_SALES_UPDATE;
        if (!TenantPermissionContext.hasPermission(permission)) {
            throw new BusinessException(403, message);
        }
    }

    private void requireSavePartPermission(boolean creating) {
        String permission = creating ? PermissionCatalogV3.CODE_AFTER_SALES_PART_CREATE : PermissionCatalogV3.CODE_AFTER_SALES_PART_UPDATE;
        if (!TenantPermissionContext.hasPermission(permission)) {
            throw new BusinessException(403, "当前账号没有维护配件库权限");
        }
    }
}
