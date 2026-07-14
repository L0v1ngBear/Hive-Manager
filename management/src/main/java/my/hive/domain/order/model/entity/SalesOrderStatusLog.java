package my.hive.domain.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 销售订单状态日志实体，用于记录销售订单从创建到发货、完成的每一次状态流转。
 */
@Data
@TableName("sales_order_status_log")
public class SalesOrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("order_id")
    private String orderId;

    @TableField("old_status")
    private String oldStatus;

    @TableField("new_status")
    private String newStatus;

    @TableField("operate_type")
    private String operateType;

    private String remark;

    private String operator;

    @TableField("operator_name")
    private String operatorName;

    @TableField("create_time")
    private LocalDateTime createTime;
}
