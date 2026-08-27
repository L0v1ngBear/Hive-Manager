package my.hive.domain.aftersales.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AfterSalesTicketSaveRequest {
    private Long id;
    @NotBlank(message = "请选择关联订单")
    private String orderId;
    @NotBlank(message = "请选择处理方式")
    private String ticketType;
    private String priority;
    @NotBlank(message = "请填写问题描述")
    private String problemDesc;
    private String contactName;
    private String contactPhone;
    private String serviceAddress;
    private String actualAddress;
    private LocalDate openingDate;
    private String diagnosis;
    private String resolution;
    private LocalDateTime scheduledTime;
    private String technicianName;
    private String waybillNo;
    private String logisticsCompany;
    private String oldMotorInfo;
    private Boolean returnOldMotor;
    private Integer returnOldMotorQuantity;
    private BigDecimal repairAmount;
    private String attachmentUrlsJson;
    private Boolean approvalRequired;
    @Valid
    private List<AfterSalesTicketPartItem> parts;

    @Data
    public static class AfterSalesTicketPartItem {
        @NotNull(message = "请选择配件")
        private Long partId;
        @NotNull(message = "请填写配件数量")
        private Integer quantity;
        private String partLocation;
        private String remark;
    }
}
