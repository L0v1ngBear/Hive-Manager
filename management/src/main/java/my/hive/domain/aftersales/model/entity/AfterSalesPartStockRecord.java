package my.hive.domain.aftersales.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sales_part_stock_record")
public class AfterSalesPartStockRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private Long partId;
    private Long ticketId;
    private Long treatmentId;
    private String operateType;
    private Integer quantity;
    private Integer beforeQty;
    private Integer afterQty;
    private Long operatorUserId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
