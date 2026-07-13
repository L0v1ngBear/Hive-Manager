package my.management.module.employee.model.enums;

import my.hive.common.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 员工类型。
 */
public enum EmployeeTypeEnum {
    FULL_TIME("FULL_TIME", "全职"),
    CONTRACT("CONTRACT", "合同工"),
    PROBATION("PROBATION", "试用期");

    private final String code;
    private final String label;

    EmployeeTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static EmployeeTypeEnum of(String code) {
        if (!StringUtils.hasText(code)) {
            return FULL_TIME;
        }
        for (EmployeeTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return FULL_TIME;
    }

    public static EmployeeTypeEnum parseCn(String value) {
        if (!StringUtils.hasText(value) || FULL_TIME.label.equals(value)) {
            return FULL_TIME;
        }
        for (EmployeeTypeEnum item : values()) {
            if (item.label.equals(value)) {
                return item;
            }
        }
        throw new BusinessException("员工类型仅支持：全职、合同工、试用期");
    }
}
