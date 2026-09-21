package my.hive.domain.order.model.entity;

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

    private String recipientName;

    private String recipientPhoneSuffix;

    /**
     * 售后关联订单时展示的客户联系人，不对应 sales_order 表字段。
     */
    @TableField(exist = false)
    private String contactName;

    /**
     * 售后关联订单时展示的客户联系人电话，不对应 sales_order 表字段。
     */
    @TableField(exist = false)
    private String contactPhone;

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

    @TableField("production_location")
    private String productionLocation;

    /**
     * 开票类型：0-未开票，1-已开票，2-其他类型。
     */
    @TableField("is_invoice")
    private Integer isInvoice;

    private String creator;

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

    /**
     * 多附件元数据 JSON；旧的三个单附件字段继续镜像第一项用于版本回退兼容。
     */
    @TableField("attachments_json")
    private String attachmentsJson;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
