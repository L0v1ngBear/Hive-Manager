package my.hive.shared.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * ImportResultVO 属于管理端后端通用能力层，定义通用输出对象。
 */
@Data
public class ImportResultVO {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private List<String> failMessages = new ArrayList<>();
}
