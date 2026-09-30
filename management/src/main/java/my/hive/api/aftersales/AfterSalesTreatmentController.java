package my.hive.api.aftersales;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.domain.aftersales.model.dto.*;
import my.hive.domain.aftersales.model.entity.AfterSalesTreatment;
import my.hive.domain.aftersales.service.AfterSalesTreatmentService;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
import my.hive.shared.annotation.*;
import my.hive.shared.dto.*;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.tenant.RequireTenantFeature;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/after-sales")
@RequireTenantFeature(TenantFeatureEnum.CODE_AFTER_SALES)
public class AfterSalesTreatmentController {
    @Resource private AfterSalesTreatmentService service;

    @GetMapping("/tickets/{ticketId}/treatments")
    @RequirePermission(PermissionCatalogV3.CODE_AFTER_SALES_DETAIL)
    public Result<List<AfterSalesTreatment>> list(@PathVariable Long ticketId) { return Result.success(service.list(ticketId)); }

    @PostMapping("/tickets/{ticketId}/treatments")
    @RequirePermission({PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, PermissionCatalogV3.CODE_AFTER_SALES_UPDATE})
    @CollectLog(module = "after_sales", action = "add_treatment", bizType = "after_sales_ticket", bizNo = "#ticketId", description = "追加售后处理记录")
    public Result<AfterSalesTreatment> create(@PathVariable Long ticketId, @Valid @RequestBody AfterSalesTreatmentSaveRequest request) {
        return Result.success(service.create(ticketId, request));
    }

    @PutMapping("/tickets/{ticketId}/treatments/{id}")
    @RequirePermission({PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, PermissionCatalogV3.CODE_AFTER_SALES_UPDATE})
    @CollectLog(module = "after_sales", action = "edit_treatment", bizType = "after_sales_ticket", bizNo = "#ticketId", description = "修改本次售后处理资料")
    public Result<AfterSalesTreatment> update(@PathVariable Long ticketId, @PathVariable Long id, @Valid @RequestBody AfterSalesTreatmentSaveRequest request) {
        return Result.success(service.update(ticketId, id, request));
    }

    @PostMapping("/tickets/{ticketId}/treatments/{id}/{action:outbound|complete|follow-up}")
    @RequirePermission({PermissionCatalogV3.CODE_AFTER_SALES_PROCESS, PermissionCatalogV3.CODE_AFTER_SALES_UPDATE, PermissionCatalogV3.CODE_AFTER_SALES_PART_OUTBOUND})
    @CollectLog(module = "after_sales", action = "advance_treatment", bizType = "after_sales_ticket", bizNo = "#ticketId", description = "处理售后出库、完成或回访")
    public Result<AfterSalesTreatment> action(@PathVariable Long ticketId, @PathVariable Long id, @PathVariable String action,
                                             @Valid @RequestBody AfterSalesTreatmentActionRequest request) {
        return Result.success(service.action(ticketId, id, action, request));
    }

    @GetMapping("/treatment-todos")
    @RequirePermission(PermissionCatalogV3.CODE_AFTER_SALES_PROCESS)
    public Result<PageResult<AfterSalesTreatment>> todos(@RequestParam(required = false) Integer pageNum,
                                                        @RequestParam(required = false) Integer pageSize) {
        return Result.success(service.myTasks(pageNum, pageSize));
    }
}
