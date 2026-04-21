package my.management.module.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水出参。
 */
@Data
public class InventoryRecordVO {

    private Long id;

    private String barcode;

    private String modelCode;

    private Integer operateType;

    private String operateTypeName;

    private BigDecimal operateMeters;

    private BigDecimal remainingMeters;

    private String operatorName;

    private LocalDateTime createTime;
}
