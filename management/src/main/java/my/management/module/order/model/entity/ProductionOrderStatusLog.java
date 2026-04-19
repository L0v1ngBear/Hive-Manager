package my.management.module.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单状态日志实体，记录状态变更轨迹。
 */
@Data
@TableName("production_order_status_log")
public class ProductionOrderStatusLog {

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

    @TableField("create_time")
    private LocalDateTime createTime;
}
