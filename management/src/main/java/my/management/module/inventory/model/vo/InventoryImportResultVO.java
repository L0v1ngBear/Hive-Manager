package my.management.module.inventory.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.management.common.vo.ImportResultVO;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryImportResultVO extends ImportResultVO {

    private Integer printTaskCount = 0;

    private List<InventoryLabelTaskVO> labelTasks = new ArrayList<>();
}
