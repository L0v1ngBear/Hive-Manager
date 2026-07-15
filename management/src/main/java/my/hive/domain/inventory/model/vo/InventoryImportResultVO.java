package my.hive.domain.inventory.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.hive.shared.dto.ImportResultVO;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryImportResultVO extends ImportResultVO {

    private Integer printTaskCount = 0;

    private List<InventoryLabelTaskVO> labelTasks = new ArrayList<>();
}