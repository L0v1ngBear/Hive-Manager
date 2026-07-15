package my.hive.api.quality;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.domain.quality.model.dto.BadProductPageRequest;
import my.hive.domain.quality.model.dto.BadProductProcessRequest;
import my.hive.domain.quality.model.dto.BadProductSaveRequest;
import my.hive.domain.quality.model.vo.BadProductVO;
import my.hive.domain.quality.service.QualityService;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.dto.PageResult;
import my.hive.shared.dto.Result;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.infrastructure.storage.BusinessAttachmentVO;
import my.hive.shared.tenant.RequireTenantFeature;
import my.hive.domain.tenant.model.enums.TenantFeatureEnum;
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

@RestController
@RequestMapping("/quality")
@RequireTenantFeature(TenantFeatureEnum.CODE_BAD_PRODUCT)
public class QualityController {

    @Resource
    private QualityService qualityService;

    @GetMapping("/list")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_LIST, message = "No permission to view quality records")
    public Result<PageResult<BadProductVO>> list(BadProductPageRequest request) {
        return Result.success(qualityService.page(request));
    }

    @PostMapping("/save")
    @RequirePermission(value = {PermissionCatalogV3.CODE_QUALITY_CREATE, PermissionCatalogV3.CODE_QUALITY_UPDATE}, message = "No permission to save quality records")
    @CollectLog(module = "bad_product", action = "save", bizType = "bad_product", bizNo = "#request.defectiveId", description = "Save quality record")
    public Result<Void> save(@Valid @RequestBody BadProductSaveRequest request) {
        qualityService.save(request);
        return Result.success(null);
    }

    @PostMapping("/attachment/upload")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_ATTACHMENT_UPLOAD, message = "No permission to upload quality attachments")
    @CollectLog(module = "bad_product", action = "upload_attachment", bizType = "bad_product_attachment", description = "Upload quality attachment")
    public Result<BusinessAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(qualityService.uploadAttachment(file));
    }

    @GetMapping("/attachment/download")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_ATTACHMENT_DOWNLOAD, message = "No permission to download quality attachments")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@RequestParam String url,
                                                                                  @RequestParam(required = false) String name) {
        org.springframework.core.io.Resource resource = qualityService.loadAttachment(url);
        String filename = name != null && !name.isBlank() ? name.trim() : resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename == null ? "quality-attachment" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping("/process")
    @RequirePermission(value = PermissionCatalogV3.CODE_QUALITY_PROCESS, message = "No permission to process quality records")
    @CollectLog(module = "bad_product", action = "process", bizType = "bad_product", bizNo = "#request.defectiveId", description = "Process quality record")
    public Result<Void> process(@Valid @RequestBody BadProductProcessRequest request) {
        qualityService.process(request);
        return Result.success(null);
    }
}
