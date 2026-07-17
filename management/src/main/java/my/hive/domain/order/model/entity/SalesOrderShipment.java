package my.hive.domain.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sales_order_shipment")
public class SalesOrderShipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("order_id")
    private String orderId;

    @TableField("logistics_company")
    private String logisticsCompany;

    @TableField("tracking_no")
    private String trackingNo;

    @TableField("sort_order")
    private Integer sortOrder;

    private Integer version;

    private String creator;

    private String updater;

    @TableField("updater_name")
    private String updaterName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
