package my.hive.shared.permission;

import my.hive.shared.context.TenantPermissionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExactPermissionRuntimeTest {

    private final PermissionCatalogV3 catalog = new PermissionCatalogV3();
    private final PermissionEvaluator evaluator = new PermissionEvaluator(catalog);

    @ParameterizedTest
    @ValueSource(strings = {"*", "order:*", "ORDER_VIEW", "order.view"})
    void rejectsNonCatalogPermissionCodes(String code) {
        assertThatThrownBy(() -> evaluator.require(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"order:list", "employee:detail", "role:permission:update"})
    void permitsOnlyAnExactGrantedCatalogLeaf(String code) {
        TenantPermissionContext.init("tenant-001", 7L, Set.of(code));
        try {
            assertThat(evaluator.require(code)).isTrue();
        } finally {
            TenantPermissionContext.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"order", "order:status", "print:label"})
    void rejectsCatalogGroupNodes(String code) {
        assertThatThrownBy(() -> evaluator.require(code)).isInstanceOf(IllegalArgumentException.class);
    }
}
