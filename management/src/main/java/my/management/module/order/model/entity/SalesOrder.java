package my.management.module.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单实体，对应销售订单主表。
 */
@Data
@TableName("sales_order")
public class SalesOrder {

    @TableId(value = "order_id", type = IdType.INPUT)
    private String orderId;

    @TableField("tenant_code")
    private String tenantCode;

    private String status;

    @TableField("customer_name")
    private String customerName;

    @TableField("customer_phone")
    private String customerPhone;

    @TableField("goods_desc")
    private String goodsDesc;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("delivery_date")
    private String deliveryDate;

    @TableField("express_company")
    private String expressCompany;

    @TableField("express_no")
    private String expressNo;

    private String creator;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
