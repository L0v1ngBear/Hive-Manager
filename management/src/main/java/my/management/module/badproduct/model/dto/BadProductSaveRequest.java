package my.management.module.badproduct.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 质量记录新增或编辑请求。
 */
@Data
public class BadProductSaveRequest {

    private String defectiveId;

    private String orderId;

    @NotBlank(message = "质量类型不能为空")
    private String type;

    @NotNull(message = "异常数量不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "异常数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "损失金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "损失金额必须大于0")
    private BigDecimal lossAmount;

    private String description;

    /**
     * 负责跟进本次质量问题的人员，便于后续追责和闭环。
     */
    private String responsiblePerson;

    /**
     * 本次质量异常的即时处理措施，例如返工、报废、让步接收等。
     */
    private String processMeasure;

    /**
     * 针对同类问题的后续改进方案，用于减少重复发生。
     */
    private String improvementPlan;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;
}
