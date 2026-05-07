package my.management.module.inventory.model.enums;

/**
 * 库存入库来源类型。
 */
public enum InventoryInTypeEnum {
    MANUAL("manual", "手动入库"),
    IMPORT_SNAPSHOT("import_snapshot", "外部库存快照导入");

    private final String code;
    private final String label;

    InventoryInTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String normalizeManual(String value) {
        return value == null || value.isBlank() ? MANUAL.code : value.trim();
    }
}
