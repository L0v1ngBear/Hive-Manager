package my.hive.domain.aftersales.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class AfterSalesTreatmentSaveRequest {
    @NotBlank @Size(max = 64)
    private String requestKey;
    private Integer version;
    @NotBlank @Pattern(regexp = "resend_parts|motor_replacement|parts_and_motor")
    private String treatmentType;
    @NotBlank @Size(max = 2000)
    private String description;
    @Size(max = 100) private String logisticsCompany;
    @Size(max = 100) private String waybillNo;
    @Size(max = 100) private String manufacturerReturnLogisticsCompany;
    @Size(max = 100) private String manufacturerReturnWaybillNo;
    @Size(max = 1000) private String oldMotorInfo;
    @Size(max = 200) private String newMotorModel;
    @Min(0) @Max(10000) private Integer motorQuantity;
    @Min(0) @Max(10000) private Integer returnOldMotorQuantity;
    @Valid @Size(max = 50)
    private List<AfterSalesTicketSaveRequest.AfterSalesTicketPartItem> parts;
    @Valid @Size(max = 9)
    private List<AfterSalesRepairImageRequest> repairImages;
}
