package my.management.module.sys.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Authoritative built-in tenant role catalog used when provisioning new tenants.
 */
@Component
public class BuiltInRoleCatalog {

    private static final String RETIRED_DASHBOARD_PERMISSION_PREFIX = "dashboard:" + "a" + "i";

    private static final Set<String> EMPLOYEE_BASELINE = immutableSet(
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

    private static final List<RoleDefinition> DEFINITIONS = List.of(
            allTenantPermissions("ADMIN", "企业负责人"),
            employeeRole("EMPLOYEE", "普通员工"),
            employeeRole("SALES_STAFF", "销售专员",
                    "customer:page", "customer:detail", "customer:add", "customer:update",
                    "price:list", "price:detail",
                    "order:list", "order:detail", "order:create",
                    "order:status:budgeting", "order:status:budget-completed",
                    "order:status:pending-confirm"),
            employeeRole("SALES_MANAGER", "销售负责人",
                    "customer:page", "customer:detail", "customer:add", "customer:update",
                    "price:list", "price:detail", "price:publish", "price:delete",
                    "order:list", "order:detail", "order:create",
                    "order:status:budgeting", "order:status:budget-completed",
                    "order:status:pending-confirm", "order:status:pending-cancel",
                    "order:status:cancelled", "order:warning:setting", "table:export"),
            employeeRole("WAREHOUSE_STAFF", "仓储专员",
                    "inventory:warning:list", "inventory:record:recent", "inventory:trend",
                    "inventory:barcode:search", "inventory:model:search",
                    "inventory:cloth:in", "inventory:cloth:out",
                    "receipt:print:list", "receipt:print:detail", "receipt:print:mark",
                    "label:template:list", "label:template:detail", "label:template:default",
                    "order:list", "order:detail", "order:status:pending-material",
                    "order:status:pending-ship", "order:status:shipped"),
            employeeRole("WAREHOUSE_MANAGER", "仓储负责人",
                    "inventory:*", "receipt:print:*", "label:template:*",
                    "order:list", "order:detail", "order:status:pending-material",
                    "order:status:pending-ship", "order:status:shipped", "table:export"),
            employeeRole("PRODUCTION_STAFF", "生产专员",
                    "order:list", "order:detail", "order:status:pending-material",
                    "order:status:producing", "order:status:pending-ship",
                    "inventory:warning:list", "inventory:model:search",
                    "badproduct:list", "badproduct:save",
                    "equipment:list", "equipment:detail", "equipment:inspection:submit"),
            employeeRole("PRODUCTION_MANAGER", "生产负责人",
                    "order:list", "order:detail", "order:status:pending-material",
                    "order:status:producing", "order:status:pending-ship",
                    "inventory:warning:list", "inventory:record:recent", "inventory:model:search",
                    "badproduct:list", "badproduct:save", "badproduct:process",
                    "equipment:list", "equipment:detail", "equipment:inspection:list",
                    "equipment:inspection:submit", "table:export"),
            employeeRole("QUALITY_STAFF", "质量专员",
                    "badproduct:list", "badproduct:save", "document:file:upload"),
            employeeRole("QUALITY_MANAGER", "质量负责人",
                    "badproduct:list", "badproduct:save", "badproduct:process",
                    "equipment:list", "equipment:detail", "equipment:inspection:list",
                    "document:file:upload", "table:export"),
            employeeRole("FINANCE_STAFF", "财务专员",
                    "approval:finance", "approval:finance:submit", "approval:finance:detail",
                    "order:list", "order:detail", "order:status:pending-pay",
                    "price:list", "price:detail"),
            employeeRole("FINANCE_MANAGER", "财务负责人",
                    "approval:finance", "approval:finance:submit", "approval:finance:detail",
                    "approval:finance:audit", "order:list", "order:detail",
                    "order:status:pending-pay", "order:status:pending-material",
                    "price:list", "price:detail", "price:publish", "price:delete", "table:export"),
            employeeRole("HR_STAFF", "人事专员",
                    "employee:list", "employee:detail", "attendance:record:list",
                    "approval:leave", "approval:leave:detail",
                    "approval:resignation", "approval:resignation:detail"),
            employeeRole("HR_MANAGER", "人事负责人",
                    "employee:*", "attendance:*",
                    "approval:leave", "approval:leave:detail", "approval:leave:audit",
                    "approval:resignation", "approval:resignation:detail", "approval:resignation:audit",
                    "notification:announcement:publish", "table:export"),
            employeeRole("INSTALLATION_STAFF", "安装专员",
                    "installation:list", "installation:update",
                    "installation:attachment:upload", "installation:attachment:download",
                    "order:list", "order:detail", "order:status:shipped", "order:status:completed"),
            employeeRole("INSTALLATION_MANAGER", "安装负责人",
                    "installation:*", "order:list", "order:detail",
                    "order:status:pending-ship", "order:status:shipped",
                    "order:status:completed", "table:export"),
            employeeRole("APPROVAL_MANAGER", "审批负责人",
                    "approval:leave", "approval:leave:detail", "approval:leave:audit",
                    "approval:finance", "approval:finance:detail", "approval:finance:audit",
                    "approval:resignation", "approval:resignation:detail", "approval:resignation:audit",
                    "approval:order:audit", "order:list", "order:detail",
                    "order:status:pending-confirm", "order:status:pending-pay",
                    "order:status:pending-cancel", "badproduct:list", "badproduct:process",
                    "table:export"),
            employeeRole("DOCUMENT_MANAGER", "文档负责人",
                    "document:list", "document:breadcrumbs", "document:folder:create",
                    "document:file:upload", "document:rename", "document:move", "table:export"),
            employeeRole("EQUIPMENT_STAFF", "设备巡检员",
                    "equipment:list", "equipment:detail", "equipment:inspection:submit"),
            employeeRole("EQUIPMENT_MANAGER", "设备负责人",
                    "equipment:list", "equipment:detail", "equipment:save",
                    "equipment:inspection:list", "equipment:inspection:submit", "table:export")
    );

    private static final Map<String, RoleDefinition> BY_CODE = indexByCode(DEFINITIONS);

    public List<RoleDefinition> definitions() {
        return DEFINITIONS;
    }

    public Set<String> employeeBaselinePermissions() {
        return EMPLOYEE_BASELINE;
    }

    public RoleDefinition require(String roleCode) {
        RoleDefinition definition = BY_CODE.get(roleCode);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown built-in role: " + roleCode);
        }
        return definition;
    }

    public boolean isTenantPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return !Set.of("*", "*:*", "super", "developer:super", "platform").contains(permissionCode)
                && !permissionCode.startsWith("platform:")
                && !permissionCode.startsWith(RETIRED_DASHBOARD_PERMISSION_PREFIX);
    }

    private static RoleDefinition allTenantPermissions(String code, String name) {
        return new RoleDefinition(code, name, Set.of(), true);
    }

    private static RoleDefinition employeeRole(String code, String name, String... permissions) {
        LinkedHashSet<String> result = new LinkedHashSet<>(EMPLOYEE_BASELINE);
        Collections.addAll(result, permissions);
        return new RoleDefinition(code, name, result, false);
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
