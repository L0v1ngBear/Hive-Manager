package my.hive.domain.manual.model.vo;

import lombok.Data;

@Data
public class TenantManualVO {

    private String content;

    private String savedAt;

    private Long updaterId;
}
