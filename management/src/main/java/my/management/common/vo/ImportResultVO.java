package my.management.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private List<String> failMessages = new ArrayList<>();
}
