package my.hive.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Receives video slices durably on the application volume. Completion streams
 * their ordered contents into the configured storage provider, so neither the
 * browser nor the JVM needs to hold the full video in memory.
 */
@Slf4j
@Service
public class ChunkedVideoUploadService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "m4v", "avi", "mkv", "webm", "3gp");
    private static final Set<String> MODULES = Set.of("sales-order", "bad-product", "finance", "installation-task");

    private final BusinessAttachmentService businessAttachmentService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;
    @Value("${app.upload.max-file-size-mb:200}")
    private long maxFileSizeMb;
    @Value("${app.upload.chunked-video.chunk-size-mb:5}")
    private int chunkSizeMb;
    @Value("${app.upload.chunked-video.expire-hours:24}")
    private int expireHours;

    public ChunkedVideoUploadService(BusinessAttachmentService businessAttachmentService) {
        this.businessAttachmentService = businessAttachmentService;
    }

    public ChunkedVideoUploadInitVO init(String module, ChunkedVideoUploadInitRequest request) {
        String normalizedModule = requireModule(module);
        String tenantCode = requireTenantCode();
        validateRequest(request);
        cleanupExpiredSessions();
        String requestedId = request.getUploadId();
        if (isSafeUploadId(requestedId)) {
            Path existing = sessionPath(tenantCode, requestedId);
            if (Files.isDirectory(existing) && metadataMatches(existing, normalizedModule, request)) {
                return toInitVO(requestedId, existing);
            }
        }
        String uploadId = UUID.randomUUID().toString();
        Path session = sessionPath(tenantCode, uploadId);
        try {
            Files.createDirectories(session);
            Properties metadata = new Properties();
            metadata.setProperty("tenantCode", tenantCode);
            metadata.setProperty("module", normalizedModule);
            metadata.setProperty("fileName", request.getFileName().trim());
            metadata.setProperty("contentType", StringUtils.hasText(request.getContentType()) ? request.getContentType().trim() : "application/octet-stream");
            metadata.setProperty("fileSize", String.valueOf(request.getFileSize()));
            metadata.setProperty("chunkSize", String.valueOf(chunkSizeBytes()));
            metadata.setProperty("totalParts", String.valueOf(totalParts(request.getFileSize())));
            metadata.setProperty("createdAt", String.valueOf(Instant.now().toEpochMilli()));
            writeMetadata(session, metadata);
            return toInitVO(uploadId, session);
        } catch (IOException e) {
            throw new BusinessException("无法创建视频分片上传任务，请稍后重试");
        }
    }

    public void uploadPart(String module, String uploadId, int partNumber, MultipartFile part) {
        Path session = requireSession(module, uploadId);
        Properties metadata = readMetadata(session);
        int totalParts = integer(metadata, "totalParts");
        if (partNumber < 1 || partNumber > totalParts || part == null || part.isEmpty()) {
            throw new BusinessException("视频分片参数不合法");
        }
        long maxPartSize = chunkSizeBytes();
        if (part.getSize() > maxPartSize) {
            throw new BusinessException("视频分片大小超过限制");
        }
        Path target = session.resolve(partFileName(partNumber));
        Path temporary = session.resolve(partFileName(partNumber) + ".uploading");
        try (InputStream input = part.getInputStream(); OutputStream output = Files.newOutputStream(temporary)) {
            input.transferTo(output);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new BusinessException("视频分片上传失败，请重试当前分片");
        }
    }

    public BusinessAttachmentVO complete(String module, String uploadId) {
        Path session = requireSession(module, uploadId);
        Properties metadata = readMetadata(session);
        int totalParts = integer(metadata, "totalParts");
        List<Path> parts = orderedParts(session, totalParts);
        if (parts.size() != totalParts) {
            throw new BusinessException("视频尚未上传完成，请继续上传缺失分片");
        }
        Path merged = session.resolve("completed-video");
        try (OutputStream output = Files.newOutputStream(merged)) {
            for (Path part : parts) {
                Files.copy(part, output);
            }
            if (Files.size(merged) != Long.parseLong(metadata.getProperty("fileSize"))) {
                throw new BusinessException("视频分片校验失败，请重新上传");
            }
            BusinessAttachmentVO result = businessAttachmentService.upload(new FileBackedMultipartFile(
                    merged, "file", metadata.getProperty("fileName"), metadata.getProperty("contentType")), requireModule(module));
            deleteRecursively(session);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("视频合并失败，请稍后重试");
        }
    }

    private Path requireSession(String module, String uploadId) {
        String tenant = requireTenantCode();
        if (!isSafeUploadId(uploadId)) throw new BusinessException("上传任务不存在或已过期");
        Path session = sessionPath(tenant, uploadId);
        if (!Files.isDirectory(session) || !metadataMatches(session, requireModule(module), null)) {
            throw new BusinessException("上传任务不存在或已过期");
        }
        return session;
    }

    private boolean metadataMatches(Path session, String module, ChunkedVideoUploadInitRequest request) {
        try {
            Properties metadata = readMetadata(session);
            if (!module.equals(metadata.getProperty("module")) || !requireTenantCode().equals(metadata.getProperty("tenantCode"))) return false;
            return request == null || (request.getFileSize() != null
                    && String.valueOf(request.getFileSize()).equals(metadata.getProperty("fileSize"))
                    && request.getFileName() != null && request.getFileName().trim().equals(metadata.getProperty("fileName")));
        } catch (BusinessException e) { return false; }
    }

    private ChunkedVideoUploadInitVO toInitVO(String uploadId, Path session) {
        Properties metadata = readMetadata(session);
        return ChunkedVideoUploadInitVO.builder().uploadId(uploadId).chunkSize(integer(metadata, "chunkSize"))
                .totalParts(integer(metadata, "totalParts")).uploadedParts(orderedParts(session, integer(metadata, "totalParts")).stream()
                        .map(path -> Integer.parseInt(path.getFileName().toString().substring(5, 11))).toList()).build();
    }

    private void validateRequest(ChunkedVideoUploadInitRequest request) {
        if (request == null || !StringUtils.hasText(request.getFileName()) || request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new BusinessException("视频上传参数不完整");
        }
        String extension = extensionOf(request.getFileName());
        if (!VIDEO_EXTENSIONS.contains(extension)) throw new BusinessException("仅支持视频分片上传");
        long max = Math.max(1, maxFileSizeMb) * 1024L * 1024L;
        if (request.getFileSize() > max) throw new BusinessException("视频大小不能超过 " + Math.max(1, maxFileSizeMb) + "MB");
    }

    private List<Path> orderedParts(Path session, int totalParts) {
        try (var paths = Files.list(session)) {
            return paths.filter(path -> path.getFileName().toString().matches("part-\\d{6}"))
                    .sorted(Comparator.comparing(Path::toString)).limit(totalParts).collect(Collectors.toList());
        } catch (IOException e) { throw new BusinessException("读取视频分片失败"); }
    }
    private Properties readMetadata(Path session) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(session.resolve("metadata.properties"))) { metadata.load(input); return metadata; }
        catch (IOException e) { throw new BusinessException("上传任务不存在或已过期"); }
    }
    private void writeMetadata(Path session, Properties metadata) throws IOException { try (OutputStream output = Files.newOutputStream(session.resolve("metadata.properties"))) { metadata.store(output, null); } }
    private int integer(Properties properties, String key) { try { return Integer.parseInt(properties.getProperty(key)); } catch (Exception e) { throw new BusinessException("上传任务数据异常"); } }
    private int chunkSizeBytes() { return Math.max(1, chunkSizeMb) * 1024 * 1024; }
    private int totalParts(long size) { return Math.toIntExact((size + chunkSizeBytes() - 1) / chunkSizeBytes()); }
    private String requireModule(String module) { if (!MODULES.contains(module)) throw new BusinessException("不支持的视频上传业务"); return module; }
    private String requireTenantCode() { String tenant = TenantPermissionContext.getTenantCode(); if (!StringUtils.hasText(tenant)) throw new BusinessException("组织信息缺失，无法上传视频"); return tenant.trim(); }
    private boolean isSafeUploadId(String value) { try { UUID.fromString(value); return true; } catch (Exception e) { return false; } }
    private Path sessionPath(String tenant, String uploadId) { return Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(".chunked-video").resolve(tenant.replaceAll("[^A-Za-z0-9_-]", "_")).resolve(uploadId).normalize(); }
    private String partFileName(int part) { return String.format("part-%06d", part); }
    private String extensionOf(String name) { int index = name.lastIndexOf('.'); return index < 0 ? "" : name.substring(index + 1).toLowerCase(java.util.Locale.ROOT); }
    private void cleanupExpiredSessions() { Path root = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(".chunked-video"); if (!Files.isDirectory(root)) return; Instant threshold = Instant.now().minus(Duration.ofHours(Math.max(1, expireHours))); try (var paths = Files.walk(root, 2)) { paths.filter(Files::isDirectory).filter(path -> !path.equals(root) && Files.exists(path.resolve("metadata.properties"))).filter(path -> { try { return Files.getLastModifiedTime(path).toInstant().isBefore(threshold); } catch (IOException e) { return false; } }).forEach(this::deleteRecursively); } catch (IOException e) { log.warn("cleanup expired chunked video uploads failed", e); } }
    private void deleteRecursively(Path path) { try (var paths = Files.walk(path)) { paths.sorted(Comparator.reverseOrder()).forEach(item -> { try { Files.deleteIfExists(item); } catch (IOException e) { log.warn("delete chunked video upload artifact failed, path={}", item); } }); } catch (IOException e) { log.warn("delete chunked video upload session failed, path={}", path); } }
}
