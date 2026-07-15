package my.hive.shared.security;

import my.hive.shared.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Validates upload URLs that are stored in business tables.
 */
public final class InternalUploadUrlValidator {

    private static final int MAX_URL_LENGTH = 512;

    private InternalUploadUrlValidator() {
    }

    public static String normalizeRelativeUploadPath(String value, String contextPath, String tenantCode, String module) {
        String path = normalize(value, contextPath);
        if (path == null) {
            return null;
        }
        String expectedPrefix = module + "/" + safeTenantSegment(tenantCode) + "/";
        if (!path.startsWith(expectedPrefix)) {
            throw new BusinessException("附件不存在或无权访问");
        }
        return path;
    }

    public static String normalizeStoredUploadUrl(String value, String contextPath, String tenantCode, String module) {
        String path = normalizeRelativeUploadPath(value, contextPath, tenantCode, module);
        if (path == null) {
            return null;
        }
        String context = normalizeContextPath(contextPath);
        return context + "/uploads/" + path;
    }

    public static String normalizeOptionalFinanceAttachment(String value, String tenantCode) {
        return normalizeOptionalFinanceAttachment(value, tenantCode, null);
    }

    public static String normalizeOptionalFinanceAttachment(String value, String tenantCode, String contextPath) {
        String path = normalize(value, contextPath);
        if (path == null) {
            return null;
        }
        String tenantSegment = safeTenantSegment(tenantCode);
        if (!path.startsWith("finance/" + tenantSegment + "/")
                && !path.startsWith("sales-order/" + tenantSegment + "/")) {
            throw new BusinessException("财务附件必须使用系统内上传文件，不能填写外部链接");
        }
        return "/uploads/" + path;
    }

    private static String normalize(String value, String contextPath) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String path = value.trim().replace('\\', '/');
        if (path.length() > MAX_URL_LENGTH
                || containsControl(path)
                || path.startsWith("//")
                || hasScheme(path)
                || path.contains("?")
                || path.contains("#")
                || path.contains("..")
                || path.toLowerCase(Locale.ROOT).contains("%2e")) {
            throw new BusinessException("附件地址不合法");
        }

        String context = normalizeContextPath(contextPath);
        if (StringUtils.hasText(context) && path.startsWith(context + "/uploads/")) {
            path = path.substring(context.length());
        }
        if (path.startsWith("/uploads/")) {
            path = path.substring("/uploads/".length());
        } else if (path.startsWith("uploads/")) {
            path = path.substring("uploads/".length());
        } else {
            throw new BusinessException("附件地址必须来自系统上传目录");
        }
        if (!StringUtils.hasText(path) || path.startsWith("/") || path.endsWith("/")) {
            throw new BusinessException("附件地址不合法");
        }
        return path;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasScheme(String value) {
        return value.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*");
    }

    private static String safeTenantSegment(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("租户信息缺失");
        }
        String normalized = tenantCode.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("租户信息不合法");
        }
        return normalized;
    }

    private static String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        String context = contextPath.trim();
        return context.startsWith("/") ? context : "/" + context;
    }
}
