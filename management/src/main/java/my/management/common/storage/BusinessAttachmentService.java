package my.management.common.storage;

import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.security.InternalUploadUrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BusinessAttachmentService {

    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> ALLOWED_MODULES = Set.of("sales-order", "bad-product", "finance", "inventory-recognition");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp",
            "doc", "docx", "xls", "xlsx", "csv",
            "txt", "zip", "rar", "7z"
    );

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public BusinessAttachmentVO upload(MultipartFile file, String module) {
        String normalizedModule = normalizeModule(module);
        if (file == null || file.isEmpty()) {
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

        String tenantFolder = safeTenantFolder();
        String dateFolder = LocalDate.now().format(DATE_FORMATTER);
        Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path targetDir = rootPath.resolve(normalizedModule).resolve(tenantFolder).resolve(dateFolder).normalize();
        if (!targetDir.startsWith(rootPath)) {
            throw new BusinessException("附件存储路径不合法");
        }

        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + normalizedExtension;
        Path targetPath = targetDir.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new BusinessException("附件存储路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException("附件上传失败，请稍后重试");
        }

        BusinessAttachmentVO vo = new BusinessAttachmentVO();
        vo.setFileName(originalFilename);
        vo.setFileSize(file.getSize());
        vo.setFileUrl(resolveContextPath() + "/uploads/" + normalizedModule + "/" + tenantFolder + "/" + dateFolder + "/" + storedFilename);
        return vo;
    }

    public org.springframework.core.io.Resource load(String attachmentUrl, String module) {
        String normalizedModule = normalizeModule(module);
        String relativePath = InternalUploadUrlValidator.normalizeRelativeUploadPath(
                attachmentUrl,
                resolveContextPath(),
                TenantPermissionContext.getTenantCode(),
                normalizedModule
        );
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException("附件地址不能为空");
        }

        Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path moduleRoot = rootPath.resolve(normalizedModule).normalize();
        Path targetPath = rootPath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(moduleRoot) || !Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new BusinessException("附件不存在或已被移除");
        }
        return new FileSystemResource(targetPath);
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

    private String safeTenantFolder() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("组织信息缺失，无法上传附件");
        }
        String normalized = tenantCode.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("组织信息不合法，无法上传附件");
        }
        return normalized;
    }

    private String resolveContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }
}
