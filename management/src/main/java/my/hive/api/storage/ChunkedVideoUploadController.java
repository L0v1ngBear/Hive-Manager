package my.hive.api.storage;

import jakarta.annotation.Resource;
import my.hive.infrastructure.storage.BusinessAttachmentVO;
import my.hive.infrastructure.storage.ChunkedVideoUploadInitRequest;
import my.hive.infrastructure.storage.ChunkedVideoUploadInitVO;
import my.hive.infrastructure.storage.ChunkedVideoUploadService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.Result;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/storage/chunked-video")
public class ChunkedVideoUploadController {
    private static final Map<String, String> PERMISSIONS = Map.of(
            "sales-order", PermissionCatalogV3.CODE_ORDER_CREATE,
            "bad-product", PermissionCatalogV3.CODE_QUALITY_ATTACHMENT_UPLOAD,
            "finance", PermissionCatalogV3.CODE_APPROVAL_FINANCE_SUBMIT,
            "installation-task", PermissionCatalogV3.CODE_INSTALLATION_ATTACHMENT_UPLOAD);
    @Resource private ChunkedVideoUploadService chunkedVideoUploadService;

    @PostMapping("/{module}/init")
    public Result<ChunkedVideoUploadInitVO> init(@PathVariable String module, @RequestBody ChunkedVideoUploadInitRequest request) { requirePermission(module); return Result.success(chunkedVideoUploadService.init(module, request)); }
    @PostMapping("/{module}/{uploadId}/part/{partNumber}")
    public Result<Void> part(@PathVariable String module, @PathVariable String uploadId, @PathVariable int partNumber, @RequestParam("file") MultipartFile file) { requirePermission(module); chunkedVideoUploadService.uploadPart(module, uploadId, partNumber, file); return Result.success(null); }
    @PostMapping("/{module}/{uploadId}/complete")
    public Result<BusinessAttachmentVO> complete(@PathVariable String module, @PathVariable String uploadId) { requirePermission(module); return Result.success(chunkedVideoUploadService.complete(module, uploadId)); }
    private void requirePermission(String module) { String permission = PERMISSIONS.get(module); if (permission == null || !TenantPermissionContext.hasPermission(permission)) throw new BusinessException("当前账号没有上传该视频附件的权限"); }
}
