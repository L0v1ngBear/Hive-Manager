package my.management.module.receipt.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutboundPrintItemVO {

    private Long id;

    private String barcode;

    private String modelCode;

    private Float spec;

    private Float meters;

    private BigDecimal price;

    private BigDecimal totalAmount;

    private String remark;
}