package my.hive.domain.aftersales.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sales_ticket_part")
public class AfterSalesTicketPart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private Long ticketId;
    private Long partId;
    private String partCode;
    private String partName;
    private String modelSpec;
    private String unit;
    private Integer quantity;
    private String partLocation;
    private String lineStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
