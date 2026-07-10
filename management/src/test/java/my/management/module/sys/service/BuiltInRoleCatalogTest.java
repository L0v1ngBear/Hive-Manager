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

    private final BuiltInRoleCatalog catalog = new BuiltInRoleCatalog();

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
        assertEquals(20, definitions.stream().map(BuiltInRoleCatalog.RoleDefinition::code).distinct().count());
        assertTrue(catalog.require("ADMIN").allTenantPermissions());
        assertFalse(catalog.require("EMPLOYEE").allTenantPermissions());
    }

    @Test
    void everyNonAdminRoleShouldInheritTheSameEmployeeBaseline() {
        Set<String> baseline = Set.of(
                "attendance:punch",
                "attendance:record:list",
                "approval:leave:submit",
                "approval:leave:detail",
                "approval:finance:submit",
                "approval:finance:detail",
                "approval:resignation:submit",
                "approval:resignation:detail",
                "document:list",
                "document:breadcrumbs",
                "notification:announcement:list"
        );

        assertEquals(baseline, catalog.employeeBaselinePermissions());
        assertEquals(baseline, catalog.require("EMPLOYEE").permissions());
        catalog.definitions().stream()
                .filter(definition -> !definition.code().equals("ADMIN"))
                .forEach(definition -> assertTrue(definition.permissions().containsAll(baseline), definition.code()));
    }

    @Test
    void rolePermissionsShouldUseUnifiedOrdersAndDedicatedInstallationPermissions() {
        BuiltInRoleCatalog.RoleDefinition installationStaff = catalog.require("INSTALLATION_STAFF");
        assertTrue(installationStaff.permissions().containsAll(Set.of(
                "installation:list",
                "installation:update",
                "installation:attachment:upload",
                "installation:attachment:download"
        )));
        assertTrue(catalog.require("INSTALLATION_MANAGER").permissions().contains("installation:*"));

        catalog.definitions().stream()
                .flatMap(definition -> definition.permissions().stream())
                .forEach(permission -> {
                    assertFalse(permission.startsWith("sales:order:"), permission);
                    assertFalse(permission.startsWith("production:order:"), permission);
                    assertFalse(permission.startsWith("dashboard:ai"), permission);
                });
    }

    @Test
    void salesAndProductionRolesShouldOnlySeeTheirOwnOrderStages() {
        Set<String> salesStages = Set.of(
                "order:status:budgeting",
                "order:status:budget-completed",
                "order:status:pending-confirm",
                "order:status:pending-cancel",
                "order:status:cancelled"
        );
        Set<String> productionStages = Set.of(
                "order:status:pending-material",
                "order:status:producing",
                "order:status:pending-ship"
        );

        for (String code : Set.of("SALES_STAFF", "SALES_MANAGER")) {
            Set<String> grantedStages = orderStatusPermissions(catalog.require(code));
            assertTrue(salesStages.containsAll(grantedStages), code + " has a non-sales order stage");
            assertFalse(grantedStages.contains("order:status:*"), code + " must not see every order stage");
            assertTrue(grantedStages.stream().noneMatch(productionStages::contains), code);
        }
        for (String code : Set.of("PRODUCTION_STAFF", "PRODUCTION_MANAGER")) {
            Set<String> grantedStages = orderStatusPermissions(catalog.require(code));
            assertTrue(productionStages.containsAll(grantedStages), code + " has a non-production order stage");
            assertTrue(grantedStages.stream().noneMatch(salesStages::contains), code);
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

    private Set<String> orderStatusPermissions(BuiltInRoleCatalog.RoleDefinition definition) {
        return definition.permissions().stream()
                .filter(permission -> permission.startsWith("order:status:"))
                .collect(Collectors.toSet());
    }
}
