package my.management.module.order.model.enums;

import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 销售/生产订单共享状态。
 */
public enum OrderStatusEnum {
    PENDING_CONFIRM("pending_confirm"),
    PENDING_PAY("pending_pay"),
    PENDING_MATERIAL("pending_material"),
    BUDGETING("budgeting"),
    BUDGET_COMPLETED("budget_completed"),
    PENDING_SHIP("pending_ship"),
    SHIPPED("shipped"),
    COMPLETED("completed"),
    PRODUCING("producing"),
    CANCELLED("cancelled");

    private static final Set<String> SALES_SYNC_CODES = Set.of(
            PENDING_CONFIRM.code,
            PENDING_SHIP.code,
            SHIPPED.code,
            COMPLETED.code
    );

    private final String code;

    OrderStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }

    public static String defaultIfBlank(String value, OrderStatusEnum defaultStatus) {
        return StringUtils.hasText(value) ? value : defaultStatus.code;
    }

    public static boolean supportsSalesProductionSync(String status) {
        return SALES_SYNC_CODES.contains(status);
    }
}
