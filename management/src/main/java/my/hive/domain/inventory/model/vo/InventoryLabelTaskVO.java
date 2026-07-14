package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryLabelTaskVO {

    private String barcode;

    private String modelCode;

    private BigDecimal spec;

    private BigDecimal meters;

    private String printTaskNo;

    private String printReason;
}