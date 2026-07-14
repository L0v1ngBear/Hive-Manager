package my.hive.domain.order.model.enums;

import org.springframework.util.StringUtils;

public enum OrderCategoryEnum {
    SAMPLE_ROOM("sample_room"),
    BULK("bulk"),
    REPLENISHMENT("replenishment"),
    SPECIAL_ORDER("special_order"),
    DRAWING_BUDGET("drawing_budget");

    private final String code;

    OrderCategoryEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return BULK.code;
        }
        String normalized = value.trim();
        for (OrderCategoryEnum item : values()) {
            if (item.code.equalsIgnoreCase(normalized)) {
                return item.code;
            }
        }
        return BULK.code;
    }
}
