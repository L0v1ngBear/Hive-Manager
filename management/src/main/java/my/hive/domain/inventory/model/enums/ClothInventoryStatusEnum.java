package my.hive.domain.inventory.model.enums;

import java.math.BigDecimal;

/**
 * 布匹库存状态。数据库仍保存原有数值，业务代码统一通过枚举表达语义。
 */
public enum ClothInventoryStatusEnum {
    IN_STOCK(0, "在库"),
    OUT_STOCK(1, "已出库"),
    PARTIAL_OUT(2, "部分出库"),
    UNKNOWN(-1, "未知");

    private final Integer code;
    private final String label;

    ClothInventoryStatusEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isOutStock() {
        return this == OUT_STOCK;
    }

    public static ClothInventoryStatusEnum of(Integer code) {
        if (code == null) {
            return IN_STOCK;
        }
        for (ClothInventoryStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static ClothInventoryStatusEnum fromStock(BigDecimal totalMeters, BigDecimal remainingMeters) {
        BigDecimal total = totalMeters == null ? BigDecimal.ZERO : totalMeters;
        BigDecimal remaining = remainingMeters == null ? BigDecimal.ZERO : remainingMeters;
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return OUT_STOCK;
        }
        if (total.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(total) < 0) {
            return PARTIAL_OUT;
        }
        return IN_STOCK;
    }
}