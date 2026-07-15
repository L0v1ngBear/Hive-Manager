package my.hive.domain.permission.service;

import my.hive.shared.permission.PermissionCatalogV3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionCatalogV3Test {

    private final PermissionCatalogV3 catalog = new PermissionCatalogV3();

    @Test
    void catalogContainsOnlyExactAssignableLeaves() {
        assertEquals(3L, catalog.version());
        assertTrue(catalog.leaves().contains("employee:permission:manage"));
        assertTrue(catalog.leaves().contains("order:status:pending-confirm:advance"));
        assertTrue(catalog.leaves().contains("order:scope:production:department"));
        assertTrue(catalog.leaves().contains("approval:auditor:setting"));

        assertFalse(catalog.leaves().stream().anyMatch(code -> code.equals("*") || code.contains("*")));
        assertFalse(catalog.isAssignable("order"));
        assertFalse(catalog.isAssignable("order:status:pending-confirm"));
        assertFalse(catalog.isAssignable("document:breadcrumbs"));
    }

    @Test
    void definitionsAreUniqueAndEveryAssignableDefinitionIsALeaf() {
        assertEquals(190, catalog.definitions().size());
        assertEquals(catalog.definitions().size(),
                new HashSet<>(catalog.definitions().stream()
                        .map(PermissionCatalogV3.PermissionDefinition::code)
                        .toList()).size());

        Set<String> parentCodes = catalog.definitions().stream()
                .map(PermissionCatalogV3.PermissionDefinition::parentCode)
                .filter(parent -> parent != null && !parent.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        catalog.definitions().stream()
                .filter(PermissionCatalogV3.PermissionDefinition::assignable)
                .forEach(definition -> assertFalse(parentCodes.contains(definition.code()), definition.code()));
        catalog.definitions().stream()
                .filter(definition -> definition.type() == PermissionCatalogV3.PermissionNodeType.GROUP)
                .forEach(definition -> assertFalse(definition.assignable(), definition.code()));
    }

    @Test
    void everyBuiltInRoleReferencesOnlyV3Leaves() {
        BuiltInRoleCatalog roles = new BuiltInRoleCatalog(catalog);
        roles.definitions().forEach(role -> {
            if (!role.allTenantPermissions()) {
                assertTrue(catalog.leaves().containsAll(role.permissions()), role.code());
            }
        });
    }
}
