package my.management.module.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    @TableField("order_category")
    private String orderCategory;

    @TableField("customer_name")
    private String customerName;

    @TableField("customer_phone")
    private String customerPhone;

    @TableField("project_name")
    private String projectName;

    @TableField("brand_name")
    private String brandName;

    @TableField("goods_desc")
    private String goodsDesc;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("information_channel")
    private String informationChannel;

    @TableField("express_company")
    private String expressCompany;

    @TableField("express_no")
    private String expressNo;

    /**
     * 是否开票：0-否，1-是。
     */
    @TableField("is_invoice")
    private Integer isInvoice;

    private String creator;

    private String remark;

    /**
     * 销售订单附件原始文件名。
     */
    @TableField("attachment_name")
    private String attachmentName;

    /**
     * 销售订单附件访问地址。
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 销售订单附件大小，单位字节。
     */
    @TableField("attachment_size")
    private Long attachmentSize;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
