package my.hive.shared.permission;

import my.hive.shared.context.TenantPermissionContext;
import org.springframework.stereotype.Component;

/** Canonical runtime permission decision point. */
@Component
public class PermissionEvaluator {

    private final PermissionCatalogV3 permissionCatalog;

    public PermissionEvaluator(PermissionCatalogV3 permissionCatalog) {
        this.permissionCatalog = permissionCatalog;
    }

    public boolean isAllowed(String permissionCode) {
        requireAssignable(permissionCode);
        return TenantPermissionContext.hasPermission(permissionCode);
    }

    public boolean require(String permissionCode) {
        requireAssignable(permissionCode);
        return TenantPermissionContext.hasPermission(permissionCode);
    }

    private void requireAssignable(String permissionCode) {
        if (!permissionCatalog.isAssignable(permissionCode)) {
            throw new IllegalArgumentException("Permission must be an exact assignable V3 leaf: " + permissionCode);
        }
    }
}
