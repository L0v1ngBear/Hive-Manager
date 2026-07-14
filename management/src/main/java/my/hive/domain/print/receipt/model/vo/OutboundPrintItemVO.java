package my.hive.domain.print.receipt.model.vo;

import lombok.Data;

import java.math.BigDecimal;
/**
 * OutboundPrintItemVO 属于管理端后端打印回执模块，定义出参结构。
 */
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
