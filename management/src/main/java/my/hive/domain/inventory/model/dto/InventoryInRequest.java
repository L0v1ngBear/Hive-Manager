package my.hive.domain.inventory.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 布匹入库入参。
 */
@Data
public class InventoryInRequest {

    private String barcode;

    @NotBlank(message = "型号不能为空")
    private String modelCode;

    @NotNull(message = "规格不能为空")
    @DecimalMin(value = "0.01", message = "规格必须大于0")
    private BigDecimal spec;

    @NotNull(message = "入库米数不能为空")
    @DecimalMin(value = "0.01", message = "入库米数必须大于0")
    private BigDecimal meters;

    private String inType;

    /**
     * 图片识别入库必须由人工核对后才能落库，避免识别误差直接进入库存。
     */
    private Boolean manualVerified;

    private LocalDateTime inTime;

    private Map<String, Object> customFields;
}