package my.management.module.inventory.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存分页查询入参。
 */
@Data
public class InventoryPageRequest {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Integer status;

    private BigDecimal specMin;

    private BigDecimal specMax;

    private BigDecimal remainingMin;

    private BigDecimal remainingMax;

    private LocalDate updatedStart;

    private LocalDate updatedEnd;

    /**
     * 时间排列方式：fifo=先进先出，lifo=先进后出。
     */
    private String timeOrder;
}
