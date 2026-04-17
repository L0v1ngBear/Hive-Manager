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
 * PriceChangeLog 属于管理端后端价格模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("price_change_log")
public class PriceChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long skuId;

    private String modelCode;

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private Long operatorUserId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
