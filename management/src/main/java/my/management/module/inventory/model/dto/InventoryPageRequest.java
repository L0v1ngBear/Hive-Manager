package my.management.module.inventory.model.dto;

import lombok.Data;

/**
 * 库存分页查询入参。
 */
@Data
public class InventoryPageRequest {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Integer status;
}
