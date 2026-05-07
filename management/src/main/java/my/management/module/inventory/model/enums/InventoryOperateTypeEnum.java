package my.management.module.inventory.model.enums;

/**
 * 库存流水操作类型。数据库仍保存原有数值，业务代码统一通过枚举表达语义。
 */
public enum InventoryOperateTypeEnum {
    IN(0, "入库"),
    OUT(1, "出库"),
    EXTERNAL_IMPORT(2, "外部导入"),
    UNKNOWN(-1, "未知");

    private final Integer code;
    private final String label;

    InventoryOperateTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static InventoryOperateTypeEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (InventoryOperateTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
