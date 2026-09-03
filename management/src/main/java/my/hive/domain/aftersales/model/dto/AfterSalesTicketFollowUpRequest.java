package my.hive.domain.aftersales.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSalesTicketFollowUpRequest {
    private LocalDateTime followUpTime;
    @NotBlank(message = "请选择客户满意度")
    private String satisfaction;
    private Boolean resolved;
    @NotBlank(message = "请填写回访内容")
    private String content;
    @Valid
    @Size(max = 9, message = "回访图片最多上传9张")
    private List<AfterSalesRepairImageRequest> followUpImages;
}
