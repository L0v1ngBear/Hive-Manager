package my.hive.domain.inventory.model.enums;

/**
 * 布匹质量标记。
 */
public enum ClothQualityFlagEnum {
    NORMAL(0, "正常"),
    BAD(1, "次品");

    private final Integer code;
    private final String label;

    ClothQualityFlagEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}