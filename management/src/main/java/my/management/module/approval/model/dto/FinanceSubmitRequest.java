package my.management.module.approval.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceSubmitRequest {

    @NotBlank(message = "财务类别不能为空")
    private String category;

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "0.01", message = "申请金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "申请事由不能为空")
    private String reason;

    private String attachmentUrl;
}
