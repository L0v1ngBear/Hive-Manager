package my.management.module.price.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("price_sku")
public class PriceSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String modelCode;

    private String batchNo;

    private String category;

    private String spec;

    private BigDecimal basePrice;

    private String currency;

    private LocalDate effectiveDate;

    private Integer status;

    private String imageUrl;

    private String remark;

    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}