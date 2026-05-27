package my.management.module.order.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 生产订单保存请求，同时用于管理端新建和完整编辑。
 */
@Data
public class ProductionOrderSaveRequest {

    private String salesOrderId;

    private String customerName;

    private String projectName;

    private String brandName;

    private String orderCategory;

    private String contactPhone;

    @NotBlank(message = "型号不能为空")
    private String modelCode;

    private String fabric;

    @NotNull(message = "克重不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "克重必须大于0")
    private Float weight;

    @NotNull(message = "规格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "规格必须大于0")
    private Float spec;

    private String color;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    private BigDecimal price;

    @NotBlank(message = "交付日期不能为空")
    private String deliveryDate;

    /**
     * 业务录单时间。允许成熟客户补录历史生产单时手动指定，不传时使用服务端当前时间。
     */
    private String createTime;

    private String status;

    private Integer process;

    private String remark;
}
