package my.management.module.badproduct.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 质量记录处理请求。
 */
@Data
public class BadProductProcessRequest {

    @NotBlank(message = "质量编号不能为空")
    private String defectiveId;

    @NotBlank(message = "处理方式不能为空")
    private String method;

    private String remark;
}
