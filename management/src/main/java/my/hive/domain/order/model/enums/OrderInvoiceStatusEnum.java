package my.hive.domain.order.model.enums;

import my.hive.shared.exception.BusinessException;

import java.util.Arrays;

/**
 * 订单开票处理状态。
 */
public enum OrderInvoiceStatusEnum {
    UNISSUED(0),
    ISSUED(1),
    OTHER(2);

    private final Integer code;

    OrderInvoiceStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static Integer normalize(Integer value) {
        Integer normalized = value == null ? UNISSUED.code : value;
        return Arrays.stream(values())
                .filter(status -> status.code.equals(normalized))
                .findFirst()
                .map(OrderInvoiceStatusEnum::getCode)
                .orElseThrow(() -> new BusinessException("开票类型只能为未开票、已开票或其他类型"));
    }

    public static boolean isUnissued(Integer value) {
        return UNISSUED.code.equals(normalize(value));
    }
}
