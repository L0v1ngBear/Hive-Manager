package my.management.module.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细实体，对应销售订单子项。
 */
@Data
@TableName("sales_order_detail")
public class SalesOrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private String orderId;

    /**
     * 明细表也保留租户字段，避免字段隔离模式下子表查询缺少租户条件列。
     */
    @TableField("tenant_code")
    private String tenantCode;

    @TableField("model_code")
    private String modelCode;

    /**
     * 销售明细克重来自小程序下单页，管理端新建和编辑时也沿用这一结构。
     */
    private Float weight;

    private String spec;

    private BigDecimal quantity;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
