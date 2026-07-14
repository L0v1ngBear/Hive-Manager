package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 布匹库存列表出参。
 */
@Data
public class ClothInventoryVO {

    private Long id;

    private String barcode;

    private String modelCode;

    private BigDecimal spec;

    private BigDecimal totalMeters;

    private BigDecimal remainingMeters;

    private Integer status;

    private String statusName;

    private String inType;

    private LocalDateTime inTime;

    private LocalDateTime outTime;

    private LocalDateTime updateTime;

    private Map<String, Object> customFields;
}