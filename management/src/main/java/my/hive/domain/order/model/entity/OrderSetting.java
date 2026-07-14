package my.hive.domain.order.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tenant-level order warning settings.
 */
@Data
@TableName("order_setting")
public class OrderSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Integer staleWarningDays;

    private Integer sampleRoomStaleWarningDays;

    private Integer bulkStaleWarningDays;

    private Integer replenishmentStaleWarningDays;

    private Integer drawingBudgetStaleWarningDays;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
