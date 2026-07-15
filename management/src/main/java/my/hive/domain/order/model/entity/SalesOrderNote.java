package my.hive.domain.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sales_order_note")
public class SalesOrderNote {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("order_id")
    private String orderId;

    private String content;

    @TableField("creator_user_id")
    private Long creatorUserId;

    @TableField("creator_name")
    private String creatorName;

    @TableField("updater_user_id")
    private Long updaterUserId;

    @TableField("updater_name")
    private String updaterName;

    private Integer version;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
