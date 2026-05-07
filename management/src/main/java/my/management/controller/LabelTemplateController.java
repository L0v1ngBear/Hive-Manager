package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.Result;
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
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_LIST, message = "您没有权限查看标签模板变量")
    public Result<List<LabelTemplateVariableVO>> variables(@RequestParam(required = false, defaultValue = "label") String printType) {
        return Result.success(labelTemplateService.variables(printType));
    }

    @GetMapping("/list")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_LIST, message = "您没有权限查看标签模板")
    public Result<List<LabelTemplateVO>> list(@RequestParam(required = false) String printType) {
        return Result.success(labelTemplateService.list(printType));
    }

    @GetMapping("/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_DETAIL, message = "您没有权限查看标签模板详情")
    public Result<LabelTemplateVO> detail(@PathVariable Long id) {
        return Result.success(labelTemplateService.detail(id));
    }

    @GetMapping("/default")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_LIST, message = "您没有权限查看默认标签模板")
    public Result<LabelTemplateVO> defaultTemplate(@RequestParam(required = false, defaultValue = "label") String printType) {
        return Result.success(labelTemplateService.defaultTemplate(printType));
    }

    @PostMapping("/save")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_SAVE, message = "您没有权限保存标签模板")
    public Result<LabelTemplateVO> save(@Valid @RequestBody LabelTemplateSaveRequest request) {
        return Result.success(labelTemplateService.save(request));
    }

    @PostMapping("/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_UPLOAD, message = "您没有权限上传标签模板")
    public Result<LabelTemplateVO> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false, defaultValue = "label") String printType,
                                          @RequestParam(required = false, defaultValue = "0") Integer isDefault) {
        return Result.success(labelTemplateService.upload(file, name, printType, isDefault));
    }

    @PostMapping("/{id}/default")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_DEFAULT, message = "您没有权限设置默认标签模板")
    public Result<Void> setDefault(@PathVariable Long id) {
        labelTemplateService.setDefault(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_LABEL_TEMPLATE_DISABLE, message = "您没有权限停用标签模板")
    public Result<Void> disable(@PathVariable Long id) {
        labelTemplateService.disable(id);
        return Result.success(null);
    }
}
