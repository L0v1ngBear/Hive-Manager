package my.management.module.sys.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInRoleCatalogTest {

    private final PermissionCatalogV3 permissionCatalog = new PermissionCatalogV3();
    private final BuiltInRoleCatalog catalog = new BuiltInRoleCatalog(permissionCatalog);

    @Test
    void shouldExposeTheAuthoritativeTwentyRoleMatrix() {
        List<BuiltInRoleCatalog.RoleDefinition> definitions = catalog.definitions();
        Set<String> expectedCodes = Set.of(
                "ADMIN", "EMPLOYEE",
                "SALES_STAFF", "SALES_MANAGER",
                "WAREHOUSE_STAFF", "WAREHOUSE_MANAGER",
                "PRODUCTION_STAFF", "PRODUCTION_MANAGER",
                "QUALITY_STAFF", "QUALITY_MANAGER",
                "FINANCE_STAFF", "FINANCE_MANAGER",
                "HR_STAFF", "HR_MANAGER",
                "INSTALLATION_STAFF", "INSTALLATION_MANAGER",
                "APPROVAL_MANAGER", "DOCUMENT_MANAGER",
                "EQUIPMENT_STAFF", "EQUIPMENT_MANAGER"
        );

        assertEquals(20, definitions.size());
        assertEquals(expectedCodes, definitions.stream()
                .map(BuiltInRoleCatalog.RoleDefinition::code)
                .collect(Collectors.toSet()));
        assertEquals(permissionCatalog.leaves(), catalog.require("ADMIN").permissions());
        assertTrue(catalog.require("ADMIN").allTenantPermissions());
        assertFalse(catalog.require("EMPLOYEE").allTenantPermissions());
    }

    @Test
    void everyNonAdminRoleShouldInheritTheSameEmployeeBaseline() {
        Set<String> baseline = Set.of(
                "dashboard:view",
                "notification:announcement:list",
                "attendance:punch",
                "attendance:record:list",
                "approval:list",
                "approval:leave:submit",
                "approval:leave:detail",
                "approval:finance:submit",
                "approval:finance:detail",
                "approval:resignation:submit",
                "approval:resignation:detail",
                "document:list"
        );

        assertEquals(baseline, catalog.employeeBaselinePermissions());
        assertEquals(baseline, catalog.require("EMPLOYEE").permissions());
        catalog.definitions().stream()
                .filter(definition -> !definition.code().equals("ADMIN"))
                .forEach(definition -> assertTrue(definition.permissions().containsAll(baseline), definition.code()));
    }

    @Test
    void everyRolePermissionShouldBeAnExactV3Leaf() {
        catalog.definitions().forEach(definition -> definition.permissions().forEach(permission -> {
            assertTrue(permissionCatalog.isAssignable(permission), definition.code() + ": " + permission);
            assertFalse(permission.contains("*"), permission);
            assertFalse(permission.equals("document:breadcrumbs"), permission);
            assertFalse(permission.startsWith("sales:order:"), permission);
            assertFalse(permission.startsWith("production:order:"), permission);
        }));

        BuiltInRoleCatalog.RoleDefinition installationManager = catalog.require("INSTALLATION_MANAGER");
        assertTrue(installationManager.permissions().contains("installation:export"));
        assertTrue(installationManager.permissions().contains("order:scope:installation:department"));
    }

    @Test
    void salesAndProductionRolesShouldUseSeparateResponsibilityScopes() {
        assertScope("SALES_STAFF", "order:scope:sales:self");
        assertScope("SALES_MANAGER", "order:scope:sales:department");
        assertScope("PRODUCTION_STAFF", "order:scope:production:self");
        assertScope("PRODUCTION_MANAGER", "order:scope:production:department");

        for (String code : Set.of("SALES_STAFF", "SALES_MANAGER")) {
            Set<String> permissions = catalog.require(code).permissions();
            assertTrue(permissions.stream().noneMatch(permission -> permission.startsWith("order:scope:production:")), code);
            assertFalse(permissions.contains("order:scope:tenant"), code);
            assertTrue(permissions.contains("order:status:completed:view"), code + " should track its orders end-to-end");
        }
        for (String code : Set.of("PRODUCTION_STAFF", "PRODUCTION_MANAGER")) {
            Set<String> permissions = catalog.require(code).permissions();
            assertTrue(permissions.stream().noneMatch(permission -> permission.startsWith("order:scope:sales:")), code);
            assertFalse(permissions.contains("order:scope:tenant"), code);
        }
    }

    @Test
    void catalogCollectionsShouldBeImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.definitions().add(catalog.require("EMPLOYEE")));
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.employeeBaselinePermissions().add("order:list"));
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.require("EMPLOYEE").permissions().add("order:list"));
    }

    private void assertScope(String roleCode, String expectedScope) {
        Set<String> scopes = catalog.require(roleCode).permissions().stream()
                .filter(permission -> permission.startsWith("order:scope:"))
                .collect(Collectors.toSet());
        assertEquals(Set.of(expectedScope), scopes, roleCode);
    }
}
