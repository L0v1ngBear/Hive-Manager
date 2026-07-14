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
        permissionCatalog.require(permissionCode);
        return TenantPermissionContext.hasPermission(permissionCode);
    }

    public boolean require(String permissionCode) {
        permissionCatalog.require(permissionCode);
        return TenantPermissionContext.hasPermission(permissionCode);
    }
}
