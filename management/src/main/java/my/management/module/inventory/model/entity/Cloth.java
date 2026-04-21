package my.management.module.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 布匹库存实体，对应 cloth 表。
 */
@Data
@TableName("cloth")
public class Cloth {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private String barcode;

    @TableField("model_code")
    private String modelCode;

    private BigDecimal spec;

    private BigDecimal meters;

    /**
     * 库存状态：0-在库，1-已出库，2-部分出库。
     */
    private Integer status;

    @TableField("total_meters")
    private BigDecimal totalMeters;

    @TableField("remaining_meters")
    private BigDecimal remainingMeters;

    @TableField("in_time")
    private LocalDateTime inTime;

    @TableField("out_time")
    private LocalDateTime outTime;

    @TableField("in_operator_id")
    private Long inOperatorId;

    @TableField("out_operator_id")
    private Long outOperatorId;

    @TableField("in_type")
    private String inType;

    @TableField("is_bad")
    private Integer isBad;

    @Version
    private Integer version;
}
