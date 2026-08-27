package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSalesTicketFollowUpRequest {
    private LocalDateTime followUpTime;
    @NotBlank(message = "请选择客户满意度")
    private String satisfaction;
    private Boolean resolved;
    @NotBlank(message = "请填写回访内容")
    private String content;
}
