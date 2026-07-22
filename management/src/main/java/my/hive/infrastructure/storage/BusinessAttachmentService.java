package my.hive.infrastructure.storage;

import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
public class BusinessAttachmentService {

    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_MODULES = Set.of(
            "sales-order",
            "bad-product",
            "finance",
            "inventory-recognition",
            "tenant-logo",
            "installation-task"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp",
            "doc", "docx", "xls", "xlsx", "csv",
            "txt", "zip", "rar", "7z"
    );

    @Resource
    private FileStorageProviderRouter storageRouter;

    public BusinessAttachmentVO upload(MultipartFile file, String module) {
        String normalizedModule = normalizeModule(module);
        validateFile(file);
        FileUploadResult uploadResult = storageRouter.upload(file, requireTenantCode(), normalizedModule);

        BusinessAttachmentVO vo = new BusinessAttachmentVO();
        vo.setFileName(uploadResult.getOriginalName());
        vo.setFileSize(uploadResult.getFileSize());
        vo.setFileUrl(uploadResult.getUrl());
        return vo;
    }

    public org.springframework.core.io.Resource load(String attachmentUrl, String module) {
        return storageRouter.load(attachmentUrl, requireTenantCode(), normalizeModule(module));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("请选择需要上传的附件");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException("附件大小不能超过 10MB");
        }

        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new BusinessException("仅支持 PDF、图片、Word、Excel、文本或压缩包附件");
        }
    }

    private String normalizeModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException("附件业务模块不能为空");
        }
        String normalized = module.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MODULES.contains(normalized)) {
            throw new BusinessException("不支持的附件业务模块");
        }
        return normalized;
    }

    private String normalizeFilename(String originalFilename) {
        String originalName = StringUtils.hasText(originalFilename)
                ? StringUtils.cleanPath(originalFilename.trim())
                : "attachment";
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new BusinessException("附件文件名不合法");
        }
        if (originalName.length() > 180) {
            String extension = StringUtils.getFilenameExtension(originalName);
            String suffix = StringUtils.hasText(extension) ? "." + extension : "";
            int maxBaseLength = Math.max(1, 180 - suffix.length());
            originalName = originalName.substring(0, Math.min(maxBaseLength, originalName.length())) + suffix;
        }
        return originalName;
    }

    private String requireTenantCode() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("组织信息缺失，无法访问附件");
        }
        return tenantCode.trim();
    }
}
