package my.management.module.order.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单保存请求，同时用于管理端新建和完整编辑。
 */
@Data
public class SalesOrderSaveRequest {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String customerPhone;

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    private String deliveryDate;

    private String expressCompany;

    private String expressNo;

    private String remark;

    private String status;

    private Integer createProductionOrder;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<ItemDTO> items;

    /**
     * 明细项结构和小程序下单页保持一致，减少两端心智差异。
     */
    @Data
    public static class ItemDTO {

        private Long id;

        @NotBlank(message = "型号不能为空")
        private String modelCode;

        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "数量必须大于0")
        private BigDecimal quantity;

        @NotNull(message = "克重不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "克重必须大于0")
        private Float weight;

        @NotNull(message = "规格不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "规格必须大于0")
        private Float spec;
    }
}
