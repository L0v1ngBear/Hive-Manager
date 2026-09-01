package my.hive.domain.aftersales.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import my.hive.domain.aftersales.model.vo.AfterSalesRepairImageVO;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("after_sales_ticket")
public class AfterSalesTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String ticketNo;
    private String orderId;
    private String customerName;
    private String customerPhone;
    private String projectName;
    private String contactName;
    private String contactPhone;
    private String serviceAddress;
    private String actualAddress;
    private LocalDate openingDate;
    private String ticketType;
    private String status;
    private String priority;
    private String problemDesc;
    private String diagnosis;
    private String resolution;
    private LocalDateTime scheduledTime;
    private String technicianName;
    private String waybillNo;
    private String logisticsCompany;
    private String oldMotorInfo;
    private Integer returnOldMotor;
    private Integer returnOldMotorQuantity;
    private BigDecimal repairAmount;
    private String attachmentUrlsJson;
    private Integer approvalRequired;
    private String approvalStatus;
    private Long approvalAuditorId;
    private String approvalAuditorIds;
    private Long assigneeUserId;
    private String assigneeName;
    private LocalDateTime followUpTime;
    private String followUpOperatorName;
    private String followUpSatisfaction;
    private Integer followUpResolved;
    private String followUpContent;
    private Long creatorUserId;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private List<AfterSalesTicketPart> parts;
    @TableField(exist = false)
    private List<AfterSalesRepairImageVO> repairImages;
    @TableField(exist = false)
    private Boolean canAudit;
}
