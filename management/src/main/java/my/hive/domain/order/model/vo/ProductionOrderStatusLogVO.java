package my.hive.domain.order.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单状态日志对象。
 */
@Data
public class ProductionOrderStatusLogVO {

    private Long id;

    private String oldStatus;

    private String newStatus;

    private String operateType;

    private String remark;

    private String operator;

    private String operatorName;

    private LocalDateTime createTime;
}
