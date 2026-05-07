package my.management.module.price.model.enums;

import java.time.LocalDate;

/**
 * 价格 SKU 状态。
 */
public enum PriceSkuStatusEnum {
    EXPIRED(0, "已过期"),
    ACTIVE(1, "生效中"),
    SCHEDULED(2, "待生效"),
    UNKNOWN(-1, "未知");

    private final Integer code;
    private final String label;

    PriceSkuStatusEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PriceSkuStatusEnum fromEffectiveDate(LocalDate effectiveDate) {
        return effectiveDate != null && effectiveDate.isAfter(LocalDate.now()) ? SCHEDULED : ACTIVE;
    }

    public static PriceSkuStatusEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (PriceSkuStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
