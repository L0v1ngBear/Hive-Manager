package my.hive.domain.print.receipt.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库单打印明细人工修正入参。
 */
@Data
public class OutboundPrintItemUpdateRequest {

    private Long id;

    private String barcode;

    private String modelCode;

    private BigDecimal spec;

    private BigDecimal meters;

    private BigDecimal price;

    private BigDecimal totalAmount;

    private String remark;
}
