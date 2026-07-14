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
        LinkedHashSet<String> denied = new LinkedHashSet<>();
        if (persisted != null) {
            for (String value : persisted) {
                boolean deny = value != null && value.startsWith("!");
                String code = deny ? value.substring(1) : value;
                if (!permissionCatalog.isAssignable(code)) {
                    continue;
                }
                if (deny) {
                    denied.add(code);
                } else {
                    resolved.add(code);
                }
            }
        }
        resolved.removeAll(denied);
        denied.forEach(code -> resolved.add("!" + code));
        return Collections.unmodifiableSet(resolved);
    }
}
