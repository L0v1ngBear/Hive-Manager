package my.management.module.price.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerOverrideVO {

    private Long id;

    private Long customerId;

    private String customerName;

    private BigDecimal price;
}