package my.hive.shared.security;

import my.hive.shared.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * Encodes stable internal references for private OSS objects without exposing credentials or signed URLs.
 */
public final class InternalStorageReference {

    public static final String PRIVATE_OSS_PREFIX = "/storage/private/aliyun-oss/";
    public static final String PUBLIC_TENANT_LOGO_PREFIX = "/storage/public/tenant-logo/";
    private static final int MAX_REFERENCE_LENGTH = 2048;
    private static final int MAX_OBJECT_KEY_LENGTH = 700;

    private InternalStorageReference() {
    }

    public static String privateOssReference(String contextPath, String objectKey) {
        return normalizeContextPath(contextPath) + PRIVATE_OSS_PREFIX + encodeObjectKey(objectKey);
    }

    public static String publicTenantLogoReference(String contextPath, String objectKey) {
        return normalizeContextPath(contextPath) + PUBLIC_TENANT_LOGO_PREFIX + encodeObjectKey(objectKey);
    }

    public static boolean isPrivateOssReference(String value, String contextPath) {
        String path = normalizePath(value, contextPath, false);
        return path != null && path.startsWith(PRIVATE_OSS_PREFIX);
    }

    public static boolean isPublicTenantLogoReference(String value, String contextPath) {
        String path = normalizePath(value, contextPath, false);
        return path != null && path.startsWith(PUBLIC_TENANT_LOGO_PREFIX);
    }

    public static String normalizePrivateOssReference(String value,
                                                      String contextPath,
                                                      String tenantCode,
                                                      Set<String> modules) {
        String objectKey = requirePrivateOssObjectKey(value, contextPath, tenantCode, modules);
        return privateOssReference(contextPath, objectKey);
    }

    public static String requirePrivateOssObjectKey(String value,
                                                    String contextPath,
                                                    String tenantCode,
                                                    Set<String> modules) {
        String path = normalizePath(value, contextPath, true);
        if (!path.startsWith(PRIVATE_OSS_PREFIX)) {
            throw invalidReference();
        }
        String objectKey = decodeObjectKey(path.substring(PRIVATE_OSS_PREFIX.length()));
        String tenantSegment = sanitizeSegment(tenantCode);
        boolean allowed = modules != null && modules.stream()
                .filter(StringUtils::hasText)
                .map(InternalStorageReference::sanitizeSegment)
                .anyMatch(module -> containsSegmentPair(objectKey, tenantSegment, module));
        if (!allowed) {
            throw new BusinessException("附件不存在或无权访问");
        }
        return objectKey;
    }

    public static String requirePublicTenantLogoObjectKey(String value, String contextPath) {
        String path = normalizePath(value, contextPath, true);
        if (!path.startsWith(PUBLIC_TENANT_LOGO_PREFIX)) {
            throw invalidReference();
        }
        String objectKey = decodeObjectKey(path.substring(PUBLIC_TENANT_LOGO_PREFIX.length()));
        String[] segments = objectKey.split("/");
        for (int i = 1; i < segments.length - 1; i++) {
            if ("tenant-logo".equals(segments[i]) && StringUtils.hasText(segments[i - 1])) {
                return objectKey;
            }
        }
        throw new BusinessException("企业 Logo 地址不合法");
    }

    public static String publicTenantLogoPathFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw invalidReference();
        }
        return PUBLIC_TENANT_LOGO_PREFIX + token.trim();
    }

    private static String encodeObjectKey(String objectKey) {
        validateObjectKey(objectKey);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeObjectKey(String token) {
        if (!StringUtils.hasText(token) || !token.matches("[A-Za-z0-9_-]+")) {
            throw invalidReference();
        }
        try {
            String objectKey = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            validateObjectKey(objectKey);
            return objectKey;
        } catch (IllegalArgumentException exception) {
            throw invalidReference();
        }
    }

    private static void validateObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)
                || objectKey.length() > MAX_OBJECT_KEY_LENGTH
                || objectKey.startsWith("/")
                || objectKey.endsWith("/")
                || objectKey.contains("//")
                || objectKey.contains("..")
                || objectKey.contains("\\")
                || containsControl(objectKey)) {
            throw invalidReference();
        }
    }

    private static boolean containsSegmentPair(String objectKey, String tenant, String module) {
        String[] segments = objectKey.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (tenant.equals(segments[i]) && module.equals(segments[i + 1])) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePath(String value, String contextPath, boolean required) {
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw invalidReference();
            }
            return null;
        }
        String path = value.trim();
        if (path.length() > MAX_REFERENCE_LENGTH
                || containsControl(path)
                || path.contains("\\")
                || path.startsWith("//")
                || path.contains("?")
                || path.contains("#")
                || path.contains("..")
                || path.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
            if (required) {
                throw invalidReference();
            }
            return null;
        }
        String context = normalizeContextPath(contextPath);
        if (StringUtils.hasText(context) && path.startsWith(context + "/")) {
            path = path.substring(context.length());
        }
        return path;
    }

    private static String sanitizeSegment(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("组织信息缺失");
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9_-]", "-");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("组织信息不合法");
        }
        return normalized;
    }

    private static String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        String context = contextPath.trim();
        context = context.startsWith("/") ? context : "/" + context;
        return context.replaceAll("/+$", "");
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static BusinessException invalidReference() {
        return new BusinessException("文件存储地址不合法");
    }
}
