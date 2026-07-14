package my.hive.domain.inventory.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 布匹出库入参。
 */
@Data
public class InventoryOutRequest {

    @NotBlank(message = "条码不能为空")
    private String barcode;

    @NotNull(message = "出库米数不能为空")
    @DecimalMin(value = "0.01", message = "出库米数必须大于0")
    private BigDecimal meters;

    private String orderNo;
}