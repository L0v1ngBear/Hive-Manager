package my.management.module.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant_usage_meter")
public class TenantUsageMeter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("meter_type")
    private String meterType;

    @TableField("period_key")
    private String periodKey;

    @TableField("used_count")
    private Integer usedCount;

    @TableField("limit_count")
    private Integer limitCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
