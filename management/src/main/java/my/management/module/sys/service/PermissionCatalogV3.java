package my.management.module.sys.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authoritative exact-code tenant permission catalog.
 */
@Component
public class PermissionCatalogV3 {

    private static final long VERSION = 3L;
    private static final List<PermissionDefinition> DEFINITIONS = buildDefinitions();
    private static final Map<String, PermissionDefinition> BY_CODE = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(PermissionDefinition::code, definition -> definition));
    private static final Set<String> LEAVES = DEFINITIONS.stream()
            .filter(PermissionDefinition::assignable)
            .map(PermissionDefinition::code)
            .collect(Collectors.toUnmodifiableSet());

    public long version() {
        return VERSION;
    }

    public List<PermissionDefinition> definitions() {
        return DEFINITIONS;
    }

    public Set<String> leaves() {
        return LEAVES;
    }

    public boolean isAssignable(String code) {
        PermissionDefinition definition = BY_CODE.get(code);
        return definition != null && definition.assignable();
    }

    public boolean contains(String code) {
        return BY_CODE.containsKey(code);
    }

    public PermissionDefinition require(String code) {
        PermissionDefinition definition = BY_CODE.get(code);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown V3 permission: " + code);
        }
        return definition;
    }

    private static List<PermissionDefinition> buildDefinitions() {
        CatalogBuilder catalog = new CatalogBuilder();

        catalog.group("dashboard", null, "dashboard", 100, "总览大盘")
                .entry("dashboard:view", "dashboard", "dashboard", 101, "查看总览大盘");
        catalog.group("notification", null, "notification", 120, "企业通知公告")
                .entry("notification:announcement:list", "notification", "notification", 121, "查看企业通知公告")
                .action("notification:announcement:publish", "notification", "notification", 122, "发布企业通知公告");

        addOrderPermissions(catalog);
        addFlatModule(catalog, "inventory", "库存管理", 300,
                entry("list", 1, "查看库存列表"), entry("detail", 2, "查看库存详情"),
                entry("warning:list", 3, "查看库存预警"), action("warning:setting", 4, "设置库存预警"),
                entry("record:list", 5, "查看库存流水"), entry("trend", 6, "查看库存趋势"),
                entry("barcode:search", 7, "查询库存条码"), entry("model:search", 8, "查询库存型号"),
                action("cloth:in", 9, "库存入库"), action("cloth:out", 10, "库存出库"),
                action("import", 11, "导入库存"), action("export", 12, "导出库存"));
        addPrintPermissions(catalog);
        addFlatModule(catalog, "quality", "质量管理", 400,
                entry("list", 1, "查看质量记录"), entry("detail", 2, "查看质量详情"),
                action("create", 3, "新建质量记录"), action("update", 4, "编辑质量记录"),
                action("process", 5, "处理质量记录"), action("audit", 6, "审核质量记录"),
                action("attachment:upload", 7, "上传质量附件"),
                action("attachment:download", 8, "下载质量附件"), action("export", 9, "导出质量记录"));
        addFlatModule(catalog, "customer", "客户管理", 500,
                entry("list", 1, "查看客户列表"), entry("detail", 2, "查看客户详情"),
                action("create", 3, "新建客户"), action("update", 4, "编辑客户"),
                action("delete", 5, "删除客户"), action("import", 6, "导入客户"),
                action("export", 7, "导出客户"));
        addFlatModule(catalog, "price", "价格管理", 520,
                entry("list", 1, "查看价格列表"), entry("detail", 2, "查看价格详情"),
                action("create", 3, "新建价格"), action("update", 4, "编辑价格"),
                action("publish", 5, "发布价格"), action("delete", 6, "删除价格"),
                action("import", 7, "导入价格"), action("export", 8, "导出价格"));
        addApprovalPermissions(catalog);
        addFlatModule(catalog, "installation", "安装任务", 650,
                entry("list", 1, "查看安装任务"), entry("detail", 2, "查看安装任务详情"),
                action("update", 3, "处理安装任务"), action("attachment:upload", 4, "上传安装附件"),
                action("attachment:download", 5, "下载安装附件"), action("export", 6, "导出安装任务"));
        addFlatModule(catalog, "attendance", "考勤管理", 700,
                action("punch", 1, "员工打卡"), entry("record:list", 2, "查看考勤记录"),
                entry("rule:list", 3, "查看考勤规则"), action("rule:update", 4, "设置考勤规则"),
                action("export", 5, "导出考勤"));
        addFlatModule(catalog, "equipment", "设备巡检", 760,
                entry("list", 1, "查看设备列表"), entry("detail", 2, "查看设备详情"),
                action("create", 3, "新增设备"), action("update", 4, "编辑设备"),
                action("disable", 5, "停用设备"), entry("inspection:list", 6, "查看巡检记录"),
                action("inspection:submit", 7, "提交巡检记录"), action("export", 8, "导出设备"));
        addFlatModule(catalog, "employee", "员工管理", 800,
                entry("list", 1, "查看员工列表"), entry("detail", 2, "查看员工详情"),
                action("create", 3, "新增员工"), action("update", 4, "编辑员工"),
                action("status", 5, "调整员工状态"), action("delete", 6, "删除员工"),
                action("import", 7, "导入员工"), action("export", 8, "导出员工"),
                action("permission:manage", 9, "配置员工单独权限"));
        addFlatModule(catalog, "role", "角色管理", 820,
                entry("list", 1, "查看角色列表"), action("create", 2, "新增角色"),
                action("update", 3, "编辑角色"), action("delete", 4, "删除角色"),
                entry("permission:list", 5, "查看角色权限"),
                action("permission:update", 6, "配置角色权限"));
        addFlatModule(catalog, "document", "文档管理", 900,
                entry("list", 1, "查看文档"), action("folder:create", 2, "新建文件夹"),
                action("file:upload", 3, "上传文件"), action("file:download", 4, "下载文件"),
                action("rename", 5, "重命名文档"), action("move", 6, "移动文档"),
                action("delete", 7, "删除文档"), action("export", 8, "导出文档"));

        return catalog.build();
    }

    private static void addOrderPermissions(CatalogBuilder catalog) {
        catalog.group("order", null, "order", 200, "订单管理")
                .entry("order:list", "order", "order", 201, "查看订单列表")
                .entry("order:detail", "order", "order", 202, "查看订单详情")
                .action("order:create", "order", "order", 203, "新建订单")
                .action("order:update", "order", "order", 204, "编辑订单")
                .action("order:print", "order", "order", 205, "打印订单")
                .entry("order:warning:list", "order", "order", 206, "查看订单预警")
                .action("order:warning:setting", "order", "order", 207, "设置订单预警")
                .group("order:audit", "order", "order", 220, "订单审核")
                .action("order:audit:shipment", "order:audit", "order", 221, "审核发货申请")
                .action("order:audit:cancel", "order:audit", "order", 222, "审核取消申请")
                .group("order:scope", "order", "order", 230, "订单数据范围")
                .scope("order:scope:sales:self", "order:scope", "order", 231, "本人销售订单")
                .scope("order:scope:sales:department", "order:scope", "order", 232, "本部门销售订单")
                .scope("order:scope:production:self", "order:scope", "order", 233, "本人生产订单")
                .scope("order:scope:production:department", "order:scope", "order", 234, "本部门生产订单")
                .scope("order:scope:assigned", "order:scope", "order", 235, "分配给我的订单")
                .scope("order:scope:installation:department", "order:scope", "order", 236, "本部门安装订单")
                .scope("order:scope:tenant", "order:scope", "order", 237, "全部租户订单")
                .group("order:status", "order", "order", 240, "订单状态权限");

        addOrderState(catalog, "budgeting", "图纸预算中", 241,
                state("view", 2411, "查看图纸预算中订单"), state("advance", 2412, "完成图纸预算"),
                state("cancel", 2413, "取消图纸预算订单"));
        addOrderState(catalog, "budget-completed", "预算已完成", 242,
                state("view", 2421, "查看预算已完成订单"));
        addOrderState(catalog, "pending-confirm", "待确认", 243,
                state("view", 2431, "查看待确认订单"), state("advance", 2432, "推进待确认订单"),
                state("cancel", 2433, "取消待确认订单"));
        addOrderState(catalog, "pending-pay", "待收款", 244,
                state("view", 2441, "查看待收款订单"), state("advance", 2442, "推进待收款订单"),
                state("rollback", 2443, "回退待收款订单"), state("cancel", 2444, "取消待收款订单"));
        addOrderState(catalog, "pending-material", "备料中", 245,
                state("view", 2451, "查看备料中订单"), state("advance", 2452, "推进备料中订单"),
                state("rollback", 2453, "回退备料中订单"), state("cancel", 2454, "取消备料中订单"));
        addOrderState(catalog, "producing", "生产中", 246,
                state("view", 2461, "查看生产中订单"), state("advance", 2462, "推进生产中订单"),
                state("rollback", 2463, "回退生产中订单"), state("cancel", 2464, "取消生产中订单"));
        addOrderState(catalog, "pending-ship", "待发货", 247,
                state("view", 2471, "查看待发货订单"), state("advance", 2472, "申请发货"),
                state("rollback", 2473, "回退待发货订单"), state("cancel", 2474, "取消待发货订单"));
        addOrderState(catalog, "shipped", "已发货", 248,
                state("view", 2481, "查看已发货订单"), state("advance", 2482, "完成已发货订单"),
                state("rollback", 2483, "回退已发货订单"), state("cancel", 2484, "取消已发货订单"));
        addOrderState(catalog, "completed", "已完成", 249,
                state("view", 2491, "查看已完成订单"), state("rollback", 2492, "回退已完成订单"));
        addOrderState(catalog, "pending-cancel", "取消审核中", 250,
                state("view", 2501, "查看取消审核中订单"));
        addOrderState(catalog, "cancelled", "已取消", 251,
                state("view", 2511, "查看已取消订单"));
    }

    private static void addOrderState(CatalogBuilder catalog,
                                      String status,
                                      String name,
                                      int sort,
                                      StateLeaf... actions) {
        String parent = "order:status:" + status;
        catalog.group(parent, "order:status", "order", sort, name);
        for (StateLeaf action : actions) {
            catalog.stateAction(parent + ":" + action.suffix(), parent, "order", action.sort(), action.name());
        }
    }

    private static void addPrintPermissions(CatalogBuilder catalog) {
        catalog.group("print", null, "print", 320, "打印管理")
                .group("print:receipt", "print", "print", 321, "出库单打印")
                .entry("print:receipt:list", "print:receipt", "print", 322, "查看出库单")
                .entry("print:receipt:detail", "print:receipt", "print", 323, "查看出库单详情")
                .action("print:receipt:execute", "print:receipt", "print", 324, "执行出库打印")
                .action("print:receipt:update", "print:receipt", "print", 325, "修正出库打印")
                .action("print:receipt:cancel", "print:receipt", "print", 326, "取消出库打印")
                .group("print:label", "print", "print", 330, "标签打印")
                .entry("print:label:list", "print:label", "print", 331, "查看标签模板")
                .entry("print:label:detail", "print:label", "print", 332, "查看标签模板详情")
                .action("print:label:create", "print:label", "print", 333, "新建标签模板")
                .action("print:label:update", "print:label", "print", 334, "编辑标签模板")
                .action("print:label:upload", "print:label", "print", 335, "上传标签模板")
                .action("print:label:default", "print:label", "print", 336, "设置默认标签模板")
                .action("print:label:disable", "print:label", "print", 337, "停用标签模板");
    }

    private static void addApprovalPermissions(CatalogBuilder catalog) {
        catalog.group("approval", null, "approval", 600, "审批中心")
                .entry("approval:list", "approval", "approval", 601, "进入审批中心");
        addApprovalType(catalog, "leave", "请假审批", 610);
        addApprovalType(catalog, "finance", "财务审批", 620);
        addApprovalType(catalog, "resignation", "离职审批", 630);
        catalog.group("approval:auditor", "approval", "approval", 640, "审核人配置")
                .entry("approval:auditor:list", "approval:auditor", "approval", 641, "查看审核人")
                .action("approval:auditor:setting", "approval:auditor", "approval", 642, "设置审核人");
    }

    private static void addApprovalType(CatalogBuilder catalog, String type, String name, int sort) {
        String parent = "approval:" + type;
        catalog.group(parent, "approval", "approval", sort, name)
                .entry(parent + ":list", parent, "approval", sort + 1, "查看" + name)
                .action(parent + ":submit", parent, "approval", sort + 2, "提交" + name.replace("审批", "申请"))
                .entry(parent + ":detail", parent, "approval", sort + 3, "查看" + name.replace("审批", "详情"))
                .action(parent + ":audit", parent, "approval", sort + 4, "审核" + name.replace("审批", "申请"));
    }

    private static void addFlatModule(CatalogBuilder catalog,
                                      String module,
                                      String name,
                                      int sort,
                                      FlatLeaf... leaves) {
        catalog.group(module, null, module, sort, name);
        for (FlatLeaf leaf : leaves) {
            String code = module + ":" + leaf.suffix();
            catalog.add(code, module, module, leaf.type(), true, sort + leaf.offset(), leaf.name());
        }
    }

    private static FlatLeaf entry(String suffix, int offset, String name) {
        return new FlatLeaf(suffix, PermissionNodeType.ENTRY, offset, name);
    }

    private static FlatLeaf action(String suffix, int offset, String name) {
        return new FlatLeaf(suffix, PermissionNodeType.ACTION, offset, name);
    }

    private static StateLeaf state(String suffix, int sort, String name) {
        return new StateLeaf(suffix, sort, name);
    }

    public enum PermissionNodeType {
        GROUP(1),
        ENTRY(2),
        ACTION(3),
        STATE_ACTION(4),
        DATA_SCOPE(4);

        private final int databaseCode;

        PermissionNodeType(int databaseCode) {
            this.databaseCode = databaseCode;
        }

        public int databaseCode() {
            return databaseCode;
        }
    }

    public record PermissionDefinition(String code,
                                       String parentCode,
                                       String moduleCode,
                                       PermissionNodeType type,
                                       boolean assignable,
                                       String name,
                                       int sort) {
        public PermissionDefinition {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(moduleCode, "moduleCode");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
        }
    }

    private record FlatLeaf(String suffix, PermissionNodeType type, int offset, String name) {
    }

    private record StateLeaf(String suffix, int sort, String name) {
    }

    private static final class CatalogBuilder {
        private final LinkedHashMap<String, PermissionDefinition> definitions = new LinkedHashMap<>();

        CatalogBuilder group(String code, String parent, String module, int sort, String name) {
            return add(code, parent, module, PermissionNodeType.GROUP, false, sort, name);
        }

        CatalogBuilder entry(String code, String parent, String module, int sort, String name) {
            return add(code, parent, module, PermissionNodeType.ENTRY, true, sort, name);
        }

        CatalogBuilder action(String code, String parent, String module, int sort, String name) {
            return add(code, parent, module, PermissionNodeType.ACTION, true, sort, name);
        }

        CatalogBuilder stateAction(String code, String parent, String module, int sort, String name) {
            return add(code, parent, module, PermissionNodeType.STATE_ACTION, true, sort, name);
        }

        CatalogBuilder scope(String code, String parent, String module, int sort, String name) {
            return add(code, parent, module, PermissionNodeType.DATA_SCOPE, true, sort, name);
        }

        CatalogBuilder add(String code,
                           String parent,
                           String module,
                           PermissionNodeType type,
                           boolean assignable,
                           int sort,
                           String name) {
            if (code.contains("*")) {
                throw new IllegalStateException("Wildcard permission is forbidden: " + code);
            }
            PermissionDefinition definition = new PermissionDefinition(
                    code, parent, module, type, assignable, name, sort);
            if (definitions.putIfAbsent(code, definition) != null) {
                throw new IllegalStateException("Duplicate V3 permission: " + code);
            }
            return this;
        }

        List<PermissionDefinition> build() {
            Set<String> parentCodes = definitions.values().stream()
                    .map(PermissionDefinition::parentCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (String parentCode : parentCodes) {
                PermissionDefinition parent = definitions.get(parentCode);
                if (parent == null) {
                    throw new IllegalStateException("Missing parent permission: " + parentCode);
                }
                if (parent.assignable()) {
                    throw new IllegalStateException("Permission parent cannot be assignable: " + parentCode);
                }
            }
            for (PermissionDefinition definition : definitions.values()) {
                if (definition.assignable() && parentCodes.contains(definition.code())) {
                    throw new IllegalStateException("Assignable permission cannot have children: " + definition.code());
                }
            }
            return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
        }
    }
}
