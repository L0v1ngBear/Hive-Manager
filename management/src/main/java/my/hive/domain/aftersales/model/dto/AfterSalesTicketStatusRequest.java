package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AfterSalesTicketStatusRequest {
    @NotNull(message = "工单不能为空")
    private Long ticketId;
    @NotBlank(message = "操作不能为空")
    private String action;
    private String resolution;
}
