package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.installation.model.dto.InstallationTaskPageRequest;
import my.management.module.installation.model.dto.InstallationTaskStatusUpdateRequest;
import my.management.module.installation.model.vo.InstallationTaskVO;
import my.management.module.installation.service.InstallationTaskService;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/installation-task")
@RequireTenantFeature(TenantFeatureEnum.CODE_ORDER)
@Validated
public class InstallationTaskController {

    @Resource
    private InstallationTaskService installationTaskService;

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_INSTALLATION_LIST, message = "您没有权限查看安装任务")
    public Result<PageResult<InstallationTaskVO>> page(InstallationTaskPageRequest request) {
        return Result.success(toPageResult(installationTaskService.page(request)));
    }

    @PostMapping("/status")
    @RequirePermission(value = PermissionCodeEnum.CODE_INSTALLATION_UPDATE, message = "您没有权限更新安装任务")
    @CollectLog(module = "installation-task", action = "update_status", bizType = "installation_task", bizNo = "#request.id", description = "管理端更新安装任务状态")
    public Result<InstallationTaskVO> updateStatus(@RequestBody @Valid InstallationTaskStatusUpdateRequest request) {
        return Result.success(installationTaskService.updateStatus(request));
    }

    @PostMapping("/attachment/upload")
    @RequirePermission(value = PermissionCodeEnum.CODE_INSTALLATION_ATTACHMENT_UPLOAD, message = "您没有权限上传安装任务附件")
    @CollectLog(module = "installation-task", action = "upload_attachment", bizType = "installation_task_attachment", description = "管理端上传安装任务附件")
    public Result<BusinessAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(installationTaskService.uploadAttachment(file));
    }

    @GetMapping("/attachment/download")
    @RequirePermission(value = PermissionCodeEnum.CODE_INSTALLATION_ATTACHMENT_DOWNLOAD, message = "您没有权限下载安装任务附件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@RequestParam String url,
                                                                                  @RequestParam(required = false) String name) {
        org.springframework.core.io.Resource resource = installationTaskService.loadAttachment(url);
        String filename = name != null && !name.isBlank() ? name.trim() : resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename == null ? "installation-task-attachment" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return result;
    }
}
