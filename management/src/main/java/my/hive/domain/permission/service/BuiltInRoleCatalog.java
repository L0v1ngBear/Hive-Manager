package my.hive.domain.permission.service;

import my.hive.shared.permission.PermissionCatalogV3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Authoritative built-in tenant roles for fresh V3 tenants.
 */
@Component
public class BuiltInRoleCatalog {

    private final PermissionCatalogV3 permissionCatalog;
    private final Set<String> employeeBaseline;
    private final List<RoleDefinition> definitions;
    private final Map<String, RoleDefinition> byCode;

    public BuiltInRoleCatalog() {
        this(new PermissionCatalogV3());
    }

    @Autowired
    public BuiltInRoleCatalog(PermissionCatalogV3 permissionCatalog) {
        this.permissionCatalog = Objects.requireNonNull(permissionCatalog, "permissionCatalog");
        this.employeeBaseline = immutableSet(
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
        this.definitions = buildDefinitions();
        this.byCode = indexByCode(definitions);
        validatePermissions();
    }

    public List<RoleDefinition> definitions() {
        return definitions;
    }

    public Set<String> employeeBaselinePermissions() {
        return employeeBaseline;
    }

    public RoleDefinition require(String roleCode) {
        RoleDefinition definition = byCode.get(roleCode);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown built-in role: " + roleCode);
        }
        return definition;
    }

    public boolean isTenantPermission(String permissionCode) {
        return permissionCatalog.isAssignable(permissionCode);
    }

    private List<RoleDefinition> buildDefinitions() {
        return List.of(
                new RoleDefinition("ADMIN", "企业负责人", permissionCatalog.leaves(), true),
                employeeRole("EMPLOYEE", "普通员工"),
                employeeRole("SALES_STAFF", "销售专员",
                        "customer:list", "customer:detail", "customer:create", "customer:update",
                        "price:list", "price:detail", "order:list", "order:detail", "order:create",
                        "order:update", "order:print", "order:scope:sales:self",
                        "order:status:budgeting:view", "order:status:budgeting:advance",
                        "order:status:budgeting:cancel", "order:status:budget-completed:view",
                        "order:status:pending-confirm:view", "order:status:pending-confirm:advance",
                        "order:status:pending-confirm:cancel", "order:status:pending-pay:view",
                        "order:status:pending-material:view", "order:status:producing:view",
                        "order:status:pending-ship:view", "order:status:shipped:view",
                        "order:status:completed:view", "order:status:pending-cancel:view",
                        "order:status:cancelled:view"),
                employeeRole("SALES_MANAGER", "销售负责人",
                        "customer:list", "customer:detail", "customer:create", "customer:update",
                        "customer:delete", "customer:import", "customer:export", "price:list",
                        "price:detail", "price:create", "price:update", "price:publish", "price:delete",
                        "price:import", "price:export", "order:list", "order:detail", "order:create",
                        "order:update", "order:print", "order:warning:list", "order:warning:setting",
                        "order:scope:sales:department", "order:status:budgeting:view",
                        "order:status:budgeting:advance", "order:status:budgeting:cancel",
                        "order:status:budget-completed:view", "order:status:pending-confirm:view",
                        "order:status:pending-confirm:advance", "order:status:pending-confirm:cancel",
                        "order:status:pending-pay:view", "order:status:pending-material:view",
                        "order:status:producing:view", "order:status:pending-ship:view",
                        "order:status:shipped:view", "order:status:completed:view",
                        "order:status:pending-cancel:view", "order:status:cancelled:view"),
                employeeRole("WAREHOUSE_STAFF", "仓储专员",
                        "inventory:list", "inventory:detail", "inventory:barcode:search",
                        "inventory:model:search", "inventory:cloth:in", "inventory:cloth:out",
                        "print:receipt:list", "print:receipt:detail", "print:receipt:execute",
                        "print:label:list", "print:label:detail", "order:list", "order:detail",
                        "order:scope:tenant", "order:status:pending-material:view",
                        "order:status:pending-ship:view", "order:status:pending-ship:advance",
                        "order:status:shipped:view"),
                employeeRole("WAREHOUSE_MANAGER", "仓储负责人",
                        "inventory:list", "inventory:detail", "inventory:warning:list",
                        "inventory:warning:setting", "inventory:record:list", "inventory:trend",
                        "inventory:barcode:search", "inventory:model:search", "inventory:cloth:in",
                        "inventory:cloth:out", "inventory:import", "inventory:export",
                        "print:receipt:list", "print:receipt:detail", "print:receipt:execute",
                        "print:receipt:update", "print:receipt:cancel", "print:label:list",
                        "print:label:detail", "print:label:create", "print:label:update",
                        "print:label:upload", "print:label:default", "print:label:disable",
                        "order:list", "order:detail", "order:scope:tenant",
                        "order:status:pending-material:view", "order:status:pending-ship:view",
                        "order:status:pending-ship:advance", "order:status:pending-ship:rollback",
                        "order:status:shipped:view"),
                employeeRole("PRODUCTION_STAFF", "生产专员",
                        "order:list", "order:detail", "order:update", "order:print",
                        "order:scope:production:self", "order:status:pending-material:view",
                        "order:status:pending-material:advance", "order:status:pending-material:rollback",
                        "order:status:producing:view", "order:status:producing:advance",
                        "order:status:producing:rollback", "order:status:pending-ship:view",
                        "order:status:pending-ship:rollback", "order:status:completed:view",
                        "quality:list", "quality:create", "equipment:list", "equipment:detail",
                        "equipment:inspection:submit"),
                employeeRole("PRODUCTION_MANAGER", "生产负责人",
                        "order:list", "order:detail", "order:update", "order:print",
                        "order:scope:production:department", "order:status:pending-material:view",
                        "order:status:pending-material:advance", "order:status:pending-material:rollback",
                        "order:status:producing:view", "order:status:producing:advance",
                        "order:status:producing:rollback", "order:status:pending-ship:view",
                        "order:status:pending-ship:rollback", "order:status:completed:view",
                        "quality:list", "quality:detail", "quality:create", "quality:update",
                        "quality:process", "quality:export", "equipment:list", "equipment:detail",
                        "equipment:inspection:list"),
                employeeRole("QUALITY_STAFF", "质量专员",
                        "quality:list", "quality:detail", "quality:create", "quality:update",
                        "quality:attachment:upload", "quality:attachment:download"),
                employeeRole("QUALITY_MANAGER", "质量负责人",
                        "quality:list", "quality:detail", "quality:create", "quality:update",
                        "quality:process", "quality:audit", "quality:attachment:upload",
                        "quality:attachment:download", "quality:export", "equipment:list",
                        "equipment:detail", "equipment:inspection:list"),
                employeeRole("FINANCE_STAFF", "财务专员",
                        "approval:finance:list", "price:list", "price:detail", "order:list",
                        "order:detail", "order:scope:tenant", "order:status:pending-pay:view",
                        "order:status:pending-pay:advance", "order:status:pending-material:view"),
                employeeRole("FINANCE_MANAGER", "财务负责人",
                        "approval:finance:list", "approval:finance:audit", "price:list", "price:detail",
                        "price:create", "price:update", "price:publish", "price:delete", "price:import",
                        "price:export", "order:list", "order:detail", "order:scope:tenant",
                        "order:status:pending-pay:view", "order:status:pending-pay:advance",
                        "order:status:pending-pay:rollback", "order:status:pending-material:view"),
                employeeRole("HR_STAFF", "人事专员",
                        "employee:list", "employee:detail", "attendance:record:list",
                        "approval:leave:list", "approval:resignation:list"),
                employeeRole("HR_MANAGER", "人事负责人",
                        "employee:list", "employee:detail", "employee:create", "employee:update",
                        "employee:status", "employee:delete", "employee:import", "employee:export",
                        "attendance:record:list", "attendance:rule:list", "attendance:rule:update",
                        "attendance:export", "approval:leave:list", "approval:leave:audit",
                        "approval:resignation:list", "approval:resignation:audit",
                        "notification:announcement:publish"),
                employeeRole("INSTALLATION_STAFF", "安装专员",
                        "installation:list", "installation:detail", "installation:update",
                        "installation:attachment:upload", "installation:attachment:download",
                        "order:list", "order:detail", "order:scope:assigned",
                        "order:status:shipped:view", "order:status:completed:view"),
                employeeRole("INSTALLATION_MANAGER", "安装负责人",
                        "installation:list", "installation:detail", "installation:update",
                        "installation:attachment:upload", "installation:attachment:download",
                        "installation:export", "order:list", "order:detail",
                        "order:scope:installation:department", "order:status:shipped:view",
                        "order:status:completed:view"),
                employeeRole("APPROVAL_MANAGER", "审批负责人",
                        "approval:leave:list", "approval:leave:audit", "approval:finance:list",
                        "approval:finance:audit", "approval:resignation:list",
                        "approval:resignation:audit", "approval:auditor:list",
                        "approval:auditor:setting", "order:list", "order:detail",
                        "order:audit:shipment", "order:audit:cancel", "order:scope:tenant",
                        "order:status:pending-ship:view", "order:status:pending-cancel:view",
                        "quality:list", "quality:detail", "quality:audit"),
                employeeRole("DOCUMENT_MANAGER", "文档负责人",
                        "document:list", "document:folder:create", "document:file:upload",
                        "document:file:download", "document:rename", "document:move",
                        "document:delete", "document:export"),
                employeeRole("EQUIPMENT_STAFF", "设备巡检员",
                        "equipment:list", "equipment:detail", "equipment:inspection:submit"),
                employeeRole("EQUIPMENT_MANAGER", "设备负责人",
                        "equipment:list", "equipment:detail", "equipment:create", "equipment:update",
                        "equipment:disable", "equipment:inspection:list",
                        "equipment:inspection:submit", "equipment:export")
        );
    }

    private RoleDefinition employeeRole(String code, String name, String... permissions) {
        LinkedHashSet<String> result = new LinkedHashSet<>(employeeBaseline);
        Collections.addAll(result, permissions);
        if (result.contains(PermissionCatalogV3.CODE_ORDER_DETAIL)) {
            result.add(PermissionCatalogV3.CODE_ORDER_NOTE_VIEW);
        }
        if (result.contains(PermissionCatalogV3.CODE_ORDER_UPDATE)) {
            result.add(PermissionCatalogV3.CODE_ORDER_NOTE_CREATE);
            result.add(PermissionCatalogV3.CODE_ORDER_NOTE_UPDATE);
        }
        if ("APPROVAL_MANAGER".equals(code)) {
            result.add(PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL);
        }
        return new RoleDefinition(code, name, result, false);
    }

    private void validatePermissions() {
        for (RoleDefinition definition : definitions) {
            if (!permissionCatalog.leaves().containsAll(definition.permissions())) {
                LinkedHashSet<String> invalid = new LinkedHashSet<>(definition.permissions());
                invalid.removeAll(permissionCatalog.leaves());
                throw new IllegalStateException("Built-in role uses non-V3 permissions: "
                        + definition.code() + " -> " + invalid);
            }
        }
    }

    private static Set<String> immutableSet(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }

    private static Map<String, RoleDefinition> indexByCode(List<RoleDefinition> definitions) {
        LinkedHashMap<String, RoleDefinition> result = new LinkedHashMap<>();
        for (RoleDefinition definition : definitions) {
            if (result.put(definition.code(), definition) != null) {
                throw new IllegalStateException("Duplicate built-in role: " + definition.code());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public record RoleDefinition(String code,
                                 String name,
                                 Set<String> permissions,
                                 boolean allTenantPermissions) {
        public RoleDefinition {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        }
    }
}
