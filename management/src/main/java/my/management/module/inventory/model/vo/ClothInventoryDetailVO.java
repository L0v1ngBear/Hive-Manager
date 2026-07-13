package my.management.module.inventory.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 单匹布详情出参，聚合布匹基础信息和库存流水。
 */
@Data
public class ClothInventoryDetailVO {

    private ClothInventoryVO cloth;

    private List<InventoryRecordVO> records;
}
