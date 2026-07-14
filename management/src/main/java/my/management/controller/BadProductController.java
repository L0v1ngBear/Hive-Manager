package my.management.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.management.common.storage.BusinessAttachmentService;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.badproduct.model.dto.BadProductPageRequest;
import my.management.module.badproduct.model.dto.BadProductProcessRequest;
import my.management.module.badproduct.model.dto.BadProductSaveRequest;
import my.management.module.badproduct.model.vo.BadProductVO;
import my.management.module.badproduct.service.BadProductService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端质量管理控制器。
 */
@RestController
@RequestMapping("/bad-product")
@RequireTenantFeature(TenantFeatureEnum.CODE_BAD_PRODUCT)
public class BadProductController {

    @Resource
    private BadProductService badProductService;

    @Resource
    private BusinessAttachmentService businessAttachmentService;

    @GetMapping("/list")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_LIST, message = "您没有权限查看质量记录列表")
    public Result<PageResult<BadProductVO>> list(BadProductPageRequest request) {
        return Result.success(badProductService.page(request));
    }

    @PostMapping("/save")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_CREATE, message = "您没有权限登记质量记录")
    @CollectLog(module = "bad_product", action = "save", bizType = "bad_product", bizNo = "#request.defectiveId", description = "管理端登记质量记录")
    public Result<Void> save(@Valid @RequestBody BadProductSaveRequest request) {
        badProductService.save(request);
        return Result.success(null);
    }

    @PostMapping("/attachment/upload")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_ATTACHMENT_UPLOAD, message = "您没有权限上传质量附件")
    @CollectLog(module = "bad_product", action = "upload_attachment", bizType = "bad_product_attachment", description = "管理端上传质量/售后附件")
    public Result<BusinessAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(businessAttachmentService.upload(file, "bad-product"));
    }

    @GetMapping("/attachment/download")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_ATTACHMENT_DOWNLOAD, message = "您没有权限下载质量附件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@RequestParam String url,
                                                                                  @RequestParam(required = false) String name) {
        org.springframework.core.io.Resource resource = businessAttachmentService.load(url, "bad-product");
        String filename = name != null && !name.isBlank() ? name.trim() : resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename == null ? "quality-attachment" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping("/process")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_PROCESS, message = "您没有权限处理质量记录")
    @CollectLog(module = "bad_product", action = "process", bizType = "bad_product", bizNo = "#request.defectiveId", description = "管理端处理质量记录")
    public Result<Void> process(@Valid @RequestBody BadProductProcessRequest request) {
        badProductService.process(request);
        return Result.success(null);
    }
}
