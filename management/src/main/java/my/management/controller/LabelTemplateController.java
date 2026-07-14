package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.context.TenantPermissionContext;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.shared.dto.Result;
import my.hive.shared.exception.BusinessException;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.label.model.dto.LabelTemplateSaveRequest;
import my.management.module.label.model.vo.LabelTemplateVO;
import my.management.module.label.model.vo.LabelTemplateVariableVO;
import my.management.module.label.service.LabelTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端标签模板接口。
 * 管理端维护模板，小程序端读取同一张 label_template 表进行打印。
 */
@RestController
@RequestMapping("/label-template")
@RequireTenantFeature(TenantFeatureEnum.CODE_LABEL)
@Validated
public class LabelTemplateController {

    @Resource
    private LabelTemplateService labelTemplateService;

    @GetMapping("/variables")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_LIST, message = "您没有权限查看标签模板变量")
    public Result<List<LabelTemplateVariableVO>> variables(@RequestParam(required = false, defaultValue = "label") String printType) {
        return Result.success(labelTemplateService.variables(printType));
    }

    @GetMapping("/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_LIST, message = "您没有权限查看标签模板")
    public Result<List<LabelTemplateVO>> list(@RequestParam(required = false) String printType) {
        return Result.success(labelTemplateService.list(printType));
    }

    @GetMapping("/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_DETAIL, message = "您没有权限查看标签模板详情")
    public Result<LabelTemplateVO> detail(@PathVariable Long id) {
        return Result.success(labelTemplateService.detail(id));
    }

    @GetMapping("/default")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_LIST, message = "您没有权限查看默认标签模板")
    public Result<LabelTemplateVO> defaultTemplate(@RequestParam(required = false, defaultValue = "label") String printType) {
        return Result.success(labelTemplateService.defaultTemplate(printType));
    }

    @PostMapping("/save")
    @CollectLog(module = "label_template", action = "save", bizType = "label_template", bizNo = "#request.id", description = "管理端保存标签模板")
    public Result<LabelTemplateVO> save(@Valid @RequestBody LabelTemplateSaveRequest request) {
        assertPermission(request.getId() == null
                ? PermissionCodeEnum.CODE_PRINT_LABEL_CREATE
                : PermissionCodeEnum.CODE_PRINT_LABEL_UPDATE, "您没有权限保存标签模板");
        return Result.success(labelTemplateService.save(request));
    }

    @PostMapping("/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_UPLOAD, message = "您没有权限上传标签模板")
    @CollectLog(module = "label_template", action = "upload", bizType = "label_template", description = "管理端上传标签模板")
    public Result<LabelTemplateVO> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false, defaultValue = "label") String printType,
                                          @RequestParam(required = false, defaultValue = "0") Integer isDefault) {
        return Result.success(labelTemplateService.upload(file, name, printType, isDefault));
    }

    @PostMapping("/{id}/default")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_DEFAULT, message = "您没有权限设置默认标签模板")
    @CollectLog(module = "label_template", action = "set_default", bizType = "label_template", bizNo = "#id", description = "管理端设置默认标签模板")
    public Result<Void> setDefault(@PathVariable Long id) {
        labelTemplateService.setDefault(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_PRINT_LABEL_DISABLE, message = "您没有权限停用标签模板")
    @CollectLog(module = "label_template", action = "disable", bizType = "label_template", bizNo = "#id", description = "管理端停用标签模板")
    public Result<Void> disable(@PathVariable Long id) {
        labelTemplateService.disable(id);
        return Result.success(null);
    }

    private void assertPermission(String permissionCode, String message) {
        if (!TenantPermissionContext.hasPermission(permissionCode)) {
            throw new BusinessException(403, message);
        }
    }
}
