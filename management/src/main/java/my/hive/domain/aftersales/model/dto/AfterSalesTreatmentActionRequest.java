package my.hive.domain.aftersales.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AfterSalesTreatmentActionRequest {
    @NotNull @Min(0) private Integer version;
    @Size(max = 2000) private String resolution;
    @Valid private AfterSalesTicketFollowUpRequest followUp;
}
