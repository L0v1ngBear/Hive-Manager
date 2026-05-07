package my.management.module.document.model.enums;

/**
 * 文档上传状态。
 */
public enum DocumentUploadStatusEnum {
    UPLOADED("UPLOADED");

    private final String code;

    DocumentUploadStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
