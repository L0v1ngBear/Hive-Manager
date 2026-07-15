package my.hive.domain.employee.model.enums;

import my.hive.shared.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 员工状态。
 */
public enum EmployeeStatusEnum {
    RESIGNED(0, "RESIGNED", "离职"),
    ACTIVE(1, "ACTIVE", "在职"),
    PROBATION(2, "PROBATION", "试用"),
    UNKNOWN(-1, "UNKNOWN", "未知");

    private final Integer code;
    private final String label;
    private final String cnLabel;

    EmployeeStatusEnum(Integer code, String label, String cnLabel) {
        this.code = code;
        this.label = label;
        this.cnLabel = cnLabel;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getCnLabel() {
        return cnLabel;
    }

    public static EmployeeStatusEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (EmployeeStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static EmployeeStatusEnum parseCn(String value) {
        if (!StringUtils.hasText(value) || ACTIVE.cnLabel.equals(value)) {
            return ACTIVE;
        }
        for (EmployeeStatusEnum item : values()) {
            if (item.cnLabel.equals(value)) {
                return item;
            }
        }
        throw new BusinessException("状态仅支持：在职、试用、离职");
    }
}
