package my.hive.shared.tenant;

import my.hive.shared.context.TenantPermissionContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Instance-level facade over the request-scoped tenant context. */
@Component
public class TenantContext {

    public void initialize(String tenantCode, Long userId, Set<String> permissions) {
        TenantPermissionContext.init(tenantCode, userId, permissions);
    }

    public String tenantCode() {
        return TenantPermissionContext.getTenantCode();
    }

    public Long userId() {
        return TenantPermissionContext.getUserId();
    }

    public void clear() {
        TenantPermissionContext.clear();
    }
}
