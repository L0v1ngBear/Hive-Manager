package my.hive.domain.aftersales.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import my.hive.domain.aftersales.model.dto.AfterSalesTreatmentSaveRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketFollowUpRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("after_sales_treatment")
public class AfterSalesTreatment {
    @TableId(type = IdType.AUTO) private Long id;
    private String tenantCode;
    private Long ticketId;
    private String ticketNo;
    private Integer sequenceNo;
    private String requestKey;
    @JsonIgnore private String requestHash;
    private String treatmentType;
    private String status;
    private Integer version;
    private LocalDate originalOpeningDate;
    private Long creatorUserId;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completedTime;
    private String resolution;
    private LocalDateTime followUpTime;
    private String followUpOperatorName;
    @JsonIgnore private String detailsJson;
    @JsonIgnore private String partsJson;
    @JsonIgnore private String followUpJson;
    @TableField(exist = false) private AfterSalesTreatmentSaveRequest details;
    @TableField(exist = false) private List<AfterSalesTicketPart> parts;
    @TableField(exist = false) private AfterSalesTicketFollowUpRequest followUp;
}
