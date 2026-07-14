package my.management.module.inventory.model.enums;

import my.hive.shared.exception.BusinessException;

/**
 * 库存入库来源类型。
 */
public enum InventoryInTypeEnum {
    MANUAL("manual", "手动入库"),
    IMPORT_SNAPSHOT("import_snapshot", "外部库存快照导入"),
    IMAGE_RECOGNITION("image_recognition", "图片识别入库");

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

    public static String normalizeInbound(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL.code;
        }
        String normalized = value.trim().toLowerCase();
        for (InventoryInTypeEnum item : values()) {
            if (item.code.equals(normalized)) {
                return item.code;
            }
        }
        throw new BusinessException("未知的入库方式");
    }
}
