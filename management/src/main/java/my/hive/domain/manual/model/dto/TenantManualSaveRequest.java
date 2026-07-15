package my.hive.domain.manual.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantManualSaveRequest {

    @Size(max = 120000, message = "使用手册内容不能超过 120000 字")
    private String content;
}
