package my.management.module.badproduct.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 次品新增或编辑请求。
 */
@Data
public class BadProductSaveRequest {

    private String defectiveId;

    private String orderId;

    @NotBlank(message = "次品类型不能为空")
    private String type;

    @NotNull(message = "次品数量不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "次品数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "损失金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "损失金额必须大于0")
    private BigDecimal lossAmount;

    private String description;

    /**
     * 负责跟进本次次品问题的人员，便于后续追责和闭环。
     */
    private String responsiblePerson;

    /**
     * 本次次品的即时处理措施，例如返工、报废、让步接收等。
     */
    private String processMeasure;

    /**
     * 针对同类问题的后续改进方案，用于减少重复发生。
     */
    private String improvementPlan;
}
