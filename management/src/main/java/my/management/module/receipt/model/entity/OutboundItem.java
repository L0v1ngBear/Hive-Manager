package my.management.module.receipt.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
/**
 * OutboundItem 属于管理端后端打印回执模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("outbound_item")
public class OutboundItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long orderId;

    private String barcode;

    private String modelCode;

    private Float spec;

    private Float meters;

    private BigDecimal price;

    private BigDecimal totalAmount;

    private String remark;

    private String requestId;
}
