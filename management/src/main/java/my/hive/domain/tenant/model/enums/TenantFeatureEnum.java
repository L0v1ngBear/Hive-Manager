package my.hive.domain.tenant.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 租户功能码目录。注解场景需要编译期常量，因此同步暴露 CODE_* 常量。
 */
public enum TenantFeatureEnum {
    DASHBOARD("module.dashboard", "总览大盘", "基础模块", "经营总览、关键指标和待办提醒", true, true),
    ORDER("module.order", "订单管理", "基础模块", "销售订单、生产订单和订单流转", true, true),
    INVENTORY("module.inventory", "库存管理", "基础模块", "库存流水、库存预警和出入库", true, true),
    BAD_PRODUCT("module.badProduct", "质量管理", "基础模块", "质量异常登记、处理闭环和损失跟踪", true, true),
    CUSTOMER("module.customer", "客户管理", "基础模块", "客户档案、联系人和合作信息", true, true),
    PRICE("module.price", "价格管理", "基础模块", "SKU 价格、客户等级价和特价", true, true),
    RECEIPT("module.receipt", "出库单打印", "基础模块", "出库单模板、打印确认和回执", true, true),
    APPROVAL("module.approval", "审批中心", "基础模块", "请假、财务等审批流程", true, true),
    ATTENDANCE("module.attendance", "考勤管理", "基础模块", "小程序打卡、规则和统计", true, true),
    EMPLOYEE("module.employee", "员工管理", "基础模块", "员工档案、组织和状态", true, true),
    EQUIPMENT("module.equipment", "设备巡检", "基础模块", "设备档案、固定巡检码和巡检记录", true, true),
    ROLE("module.role", "角色管理", "基础模块", "角色权限和人员授权", true, true),
    LABEL("module.label", "标签打印", "基础模块", "标签模板和小程序打印联动", true, true),
    DOCUMENT("module.document", "文档管理", "基础模块", "企业目录、文件和 OSS 存储", true, true),
    MANUAL("module.manual", "使用手册", "基础模块", "网页端用户使用说明", true, true);

    public static final String CODE_DASHBOARD = "module.dashboard";
    public static final String CODE_ORDER = "module.order";
    public static final String CODE_INVENTORY = "module.inventory";
    public static final String CODE_BAD_PRODUCT = "module.badProduct";
    public static final String CODE_CUSTOMER = "module.customer";
    public static final String CODE_PRICE = "module.price";
    public static final String CODE_RECEIPT = "module.receipt";
    public static final String CODE_APPROVAL = "module.approval";
    public static final String CODE_ATTENDANCE = "module.attendance";
    public static final String CODE_EMPLOYEE = "module.employee";
    public static final String CODE_EQUIPMENT = "module.equipment";
    public static final String CODE_ROLE = "module.role";
    public static final String CODE_LABEL = "module.label";
    public static final String CODE_DOCUMENT = "module.document";
    public static final String CODE_MANUAL = "module.manual";

    private final String code;
    private final String name;
    private final String category;
    private final String description;
    private final boolean baseModule;
    private final boolean defaultEnabled;

    TenantFeatureEnum(String code, String name, String category, String description, boolean baseModule, boolean defaultEnabled) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.description = description;
        this.baseModule = baseModule;
        this.defaultEnabled = defaultEnabled;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBaseModule() {
        return baseModule;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public static List<String> baseModuleCodes() {
        return Arrays.stream(values())
                .filter(TenantFeatureEnum::isBaseModule)
                .map(TenantFeatureEnum::getCode)
                .toList();
    }
}
