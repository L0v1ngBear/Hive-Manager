package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;
/**
 * CustomerOverrideVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
public class CustomerOverrideVO {

    private Long id;

    private Long customerId;

    private String customerName;

    private BigDecimal price;
}
