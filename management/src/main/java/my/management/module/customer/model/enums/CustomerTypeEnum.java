package my.management.module.customer.model.enums;

/**
 * 客户类型。
 */
public enum CustomerTypeEnum {
    DEFAULT(1);

    private final Integer code;

    CustomerTypeEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
