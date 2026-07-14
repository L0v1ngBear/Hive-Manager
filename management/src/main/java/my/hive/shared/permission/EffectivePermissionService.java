package my.hive.shared.permission;

import my.management.module.auth.mapper.AuthMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.Set;

/** Resolves effective grants from the canonical management employee/role persistence query. */
@Service
public class EffectivePermissionService {
    private final AuthMapper authMapper;
    private final PermissionCatalogV3 permissionCatalog;

    public EffectivePermissionService(AuthMapper authMapper, PermissionCatalogV3 permissionCatalog) {
        this.authMapper = authMapper;
        this.permissionCatalog = permissionCatalog;
    }

    public Set<String> resolve(long userId, String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        List<String> persisted = authMapper.selectPermCodesByUserIdAndTenantCode(userId, tenantCode);
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        if (persisted != null) {
            persisted.stream().filter(permissionCatalog::isAssignable).forEach(resolved::add);
        }
        return Collections.unmodifiableSet(resolved);
    }
}
