package my.management.module.price.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * PriceCustomerOverride 属于管理端后端价格模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("price_customer_override")
public class PriceCustomerOverride {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long skuId;

    private Long customerId;

    private String customerName;

    private BigDecimal price;

    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
