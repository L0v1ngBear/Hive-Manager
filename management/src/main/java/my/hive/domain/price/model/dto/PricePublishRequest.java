package my.hive.domain.price.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
 * PricePublishRequest 属于管理端后端价格模块，定义入参结构。
 */
@Data
public class PricePublishRequest {

    private Long id;

    @NotBlank(message = "型号不能为空")
    private String modelCode;

    private String batchNo;

    private String spec;

    @NotNull(message = "基准价不能为空")
    @DecimalMin(value = "0.01", message = "基准价必须大于0")
    private BigDecimal basePrice;

    private String currency = "CNY";

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    private String remark;

    @Valid
    private List<TierPriceRequest> tierPrices = new ArrayList<>();

    @Valid
    private List<CustomerOverrideRequest> overrides = new ArrayList<>();
}
