package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AfterSalesPartStockInRequest {
    @NotNull(message = "配件不能为空")
    private Long partId;
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    private Integer quantity;
    private String remark;
}
