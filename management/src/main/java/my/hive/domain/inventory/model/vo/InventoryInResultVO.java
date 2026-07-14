package my.hive.domain.inventory.model.vo;

import lombok.Data;

@Data
public class InventoryInResultVO {

    private ClothInventoryVO cloth;

    private InventoryLabelTaskVO labelTask;
}