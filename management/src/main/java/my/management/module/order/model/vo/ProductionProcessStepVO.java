package my.management.module.order.model.vo;

import lombok.Data;

/**
 * 生产工序进度节点，供网页端按统一口径展示订单履约进度。
 */
@Data
public class ProductionProcessStepVO {

    private Integer code;

    private String name;

    private Boolean done;

    private Boolean current;
}
