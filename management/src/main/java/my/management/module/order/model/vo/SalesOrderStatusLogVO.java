package my.management.module.order.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 销售订单状态日志出参，用于管理端和小程序端展示时间轴。
 */
@Data
public class SalesOrderStatusLogVO {

    private Long id;

    private String oldStatus;

    private String newStatus;

    private String operateType;

    private String remark;

    private String operator;

    private String operatorName;

    private LocalDateTime createTime;
}
