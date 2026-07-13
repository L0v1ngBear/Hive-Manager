package my.management.module.price.model.dto;

import lombok.Data;

import java.math.BigDecimal;
/**
 * CustomerOverrideRequest 属于管理端后端价格模块，定义入参结构。
 */
@Data
public class CustomerOverrideRequest {

    private Long customerId;

    private String customerName;

    private BigDecimal price;
}
