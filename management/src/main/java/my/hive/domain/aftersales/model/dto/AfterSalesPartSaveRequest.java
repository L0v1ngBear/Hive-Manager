package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSalesPartSaveRequest {
    private Long id;
    private String partCode;
    @NotBlank(message = "请填写配件名称")
    private String partName;
    private String category;
    private String modelSpec;
    @DecimalMin(value = "0.00", message = "单价不能小于0")
    private BigDecimal unitPrice;
    private String unit;
    @Min(value = 0, message = "安全库存不能小于0")
    private Integer safetyStock;
    private String photoUrl;
    private String remark;
    private Integer status;
}
