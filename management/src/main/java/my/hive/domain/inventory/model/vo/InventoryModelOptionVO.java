package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 型号规格选项出参。
 */
@Data
public class InventoryModelOptionVO {

    private String modelCode;

    private BigDecimal spec;
}