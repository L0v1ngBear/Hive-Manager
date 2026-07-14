package my.hive.domain.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水实体，对应 inventory_record 表。
 */
@Data
@TableName("inventory_record")
public class InventoryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("cloth_id")
    private Long clothId;

    @TableField("model_code")
    private String modelCode;

    /**
     * 操作类型：0-入库，1-出库。
     */
    @TableField("operate_type")
    private Integer operateType;

    @TableField("operate_meters")
    private BigDecimal operateMeters;

    @TableField("remaining_meters")
    private BigDecimal remainingMeters;

    @TableField("operator_id")
    private Long operatorId;
}