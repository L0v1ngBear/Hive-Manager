package my.hive.shared.permission;

import my.management.module.auth.mapper.AuthMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectivePermissionServiceTest {

    @Test
    void resolvesOnlyExactCatalogLeavesFromCanonicalManagementPersistence() {
        AuthMapper mapper = mock(AuthMapper.class);
        when(mapper.selectPermCodesByUserIdAndTenantCode(9L, "tenant-001"))
                .thenReturn(List.of("order:list", "*", "ORDER_VIEW", "order:list"));

        EffectivePermissionService service = new EffectivePermissionService(mapper, new PermissionCatalogV3());

        assertThat(service.resolve(9L, "tenant-001")).containsExactly("order:list");
    }
}
