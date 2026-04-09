package my.management.module.document;

import lombok.Getter;

@Getter
public enum DocumentTypeEnum {
    FOLDER(0),
    FILE(1);

    private final Integer type;

    DocumentTypeEnum(Integer type) {
        this.type = type;
    }
}
