package my.hive.shared.security;

import my.hive.shared.context.TenantPermissionContext;
import org.springframework.stereotype.Component;

/** Canonical runtime permission decision point. */
@Component
public class PermissionEvaluator {

    public boolean isAllowed(String permissionCode) {
        return TenantPermissionContext.hasPermission(permissionCode);
    }
}
