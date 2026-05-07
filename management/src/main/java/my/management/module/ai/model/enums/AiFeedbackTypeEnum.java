package my.management.module.ai.model.enums;

/**
 * AI 建议反馈类型及对应标注状态。
 */
public enum AiFeedbackTypeEnum {
    USEFUL("useful", "positive"),
    RESOLVED("resolved", "resolved"),
    IRRELEVANT("irrelevant", "negative"),
    IGNORED("ignored", "ignored");

    private final String code;
    private final String labelStatus;

    AiFeedbackTypeEnum(String code, String labelStatus) {
        this.code = code;
        this.labelStatus = labelStatus;
    }

    public String getCode() {
        return code;
    }

    public String getLabelStatus() {
        return labelStatus;
    }

    public boolean matches(String value) {
        return code.equalsIgnoreCase(value);
    }

    public static AiFeedbackTypeEnum of(String value) {
        if (value == null) {
            return null;
        }
        for (AiFeedbackTypeEnum item : values()) {
            if (item.matches(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
