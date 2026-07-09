package my.management.common.storage;

import lombok.extern.slf4j.Slf4j;
import my.hive.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService {

    private static final String STORAGE_PROVIDER = "LOCAL";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp",
            "doc", "docx", "xls", "xlsx", "csv",
            "txt", "zip", "rar", "7z", "ppt", "pptx"
    );

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${app.upload.max-file-size-mb:20}")
    private long maxFileSizeMb;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public FileUploadResult upload(MultipartFile file, String tenantCode, String module) {
        FileCandidate candidate = validateFile(file, tenantCode, module);
        String dateFolder = LocalDate.now().format(DATE_FORMATTER);
        String objectKey = candidate.module() + "/" + candidate.tenantCode() + "/" + dateFolder + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + candidate.fileExt();
        Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new BusinessException("文件存储路径不合法");
        }

        String fileHash = sha256(file);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("local file upload failed, tenantCode={}, objectKey={}", candidate.tenantCode(), objectKey, e);
            throw new BusinessException("文件上传失败，请稍后重试");
        }

        return FileUploadResult.builder()
                .originalName(candidate.originalName())
                .storageProvider(STORAGE_PROVIDER)
                .bucketName("")
                .objectKey(objectKey)
                .url(resolveContextPath() + "/uploads/" + objectKey)
                .fileSize(candidate.fileSize())
                .fileExt(candidate.fileExt())
                .mimeType(candidate.mimeType())
                .fileHash(fileHash)
                .etag(fileHash)
                .build();
    }

    public void deleteQuietly(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            Path rootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
            Path targetPath = rootPath.resolve(objectKey).normalize();
            if (targetPath.startsWith(rootPath)) {
                Files.deleteIfExists(targetPath);
            }
        } catch (Exception e) {
            log.warn("delete local uploaded file failed, objectKey={}", objectKey, e);
        }
    }

    private FileCandidate validateFile(MultipartFile file, String tenantCode, String module) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("组织信息缺失，无法上传文件");
        }
        if (!StringUtils.hasText(module)) {
            throw new BusinessException("文件业务模块不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的文件");
        }
        long size = file.getSize();
        if (size <= 0) {
            throw new BusinessException("文件内容为空，无法上传");
        }
        long maxBytes = Math.max(1, maxFileSizeMb) * 1024L * 1024L;
        if (size > maxBytes) {
            throw new BusinessException("文件大小不能超过 " + Math.max(1, maxFileSizeMb) + "MB");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String fileExt = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(fileExt)) {
            throw new BusinessException("不支持的文件类型：" + fileExt);
        }

        String mimeType = StringUtils.hasText(file.getContentType())
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "application/octet-stream";
        return new FileCandidate(
                sanitizePathSegment(tenantCode),
                sanitizePathSegment(module),
                originalName,
                fileExt,
                mimeType,
                size
        );
    }

    private String normalizeOriginalName(String originalFilename) {
        String originalName = StringUtils.hasText(originalFilename)
                ? StringUtils.cleanPath(originalFilename.trim())
                : "upload-file";
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new BusinessException("文件名不合法");
        }
        if (originalName.length() > 180) {
            int dotIndex = originalName.lastIndexOf('.');
            String ext = dotIndex >= 0 ? originalName.substring(dotIndex) : "";
            int maxBaseLength = Math.max(1, 180 - ext.length());
            originalName = originalName.substring(0, Math.min(maxBaseLength, originalName.length())) + ext;
        }
        return originalName;
    }

    private String extractExtension(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            throw new BusinessException("文件缺少扩展名");
        }
        return originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // DigestInputStream updates digest while streaming.
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest()) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("calculate local upload file hash failed", e);
            throw new BusinessException("文件校验失败，请重新上传");
        }
    }

    private String sanitizePathSegment(String value) {
        String normalized = value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        return StringUtils.hasText(normalized) ? normalized : "default";
    }

    private String resolveContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }

    private record FileCandidate(
            String tenantCode,
            String module,
            String originalName,
            String fileExt,
            String mimeType,
            long fileSize
    ) {
    }
}
