package my.hive.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import my.hive.shared.context.TenantPermissionContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;

/**
 * Protects uploaded business files served by Spring's static resource handler.
 * Tenant logos remain public; all other upload paths must include the current tenant segment.
 */
public class TenantUploadResourceResolver extends PathResourceResolver {

    private static final String PUBLIC_TENANT_LOGO_PREFIX = "tenant-logo/";

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request,
                                               String requestPath,
                                               List<? extends Resource> locations,
                                               ResourceResolverChain chain) {
        if (!isAllowedUploadPath(requestPath)) {
            return null;
        }
        return super.resolveResourceInternal(request, requestPath, locations, chain);
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath,
                                            List<? extends Resource> locations,
                                            ResourceResolverChain chain) {
        if (!isAllowedUploadPath(resourceUrlPath)) {
            return null;
        }
        return super.resolveUrlPathInternal(resourceUrlPath, locations, chain);
    }

    private boolean isAllowedUploadPath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return false;
        }
        String path = rawPath.trim().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!StringUtils.hasText(path)
                || path.contains("..")
                || path.contains("//")
                || path.startsWith("WEB-INF/")
                || path.startsWith("META-INF/")) {
            return false;
        }
        if (path.startsWith(PUBLIC_TENANT_LOGO_PREFIX)) {
            return true;
        }

        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!StringUtils.hasText(tenantCode)) {
            return false;
        }
        String tenantSegment = tenantCode.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (!StringUtils.hasText(tenantSegment)) {
            return false;
        }

        String[] segments = path.split("/");
        return segments.length >= 3 && tenantSegment.equals(segments[1]);
    }
}
