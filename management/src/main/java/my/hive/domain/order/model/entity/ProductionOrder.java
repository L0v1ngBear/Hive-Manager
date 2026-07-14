package my.hive.domain.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产订单实体，对应生产排单主表。
 */
@Data
@TableName("production_order")
public class ProductionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("order_id")
    private String orderId;

    @TableField("sales_order_id")
    private String salesOrderId;

    private String status;

    @TableField("order_category")
    private String orderCategory;

    @TableField("model")
    private String modelCode;

    private String fabric;

    private BigDecimal weight;

    @TableField("width")
    private BigDecimal width;

    private String color;

    private Integer quantity;

    private Integer process;

    @TableField("customer_id")
    private String customerId;

    @TableField("customer_name")
    private String customerName;

    @TableField("project_name")
    private String projectName;

    @TableField("brand_name")
    private String brandName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("information_channel")
    private String informationChannel;

    private String creator;

    private String updater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
