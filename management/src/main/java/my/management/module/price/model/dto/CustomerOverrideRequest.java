package my.management.module.price.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerOverrideRequest {

    private Long customerId;

    private String customerName;

    private BigDecimal price;
}