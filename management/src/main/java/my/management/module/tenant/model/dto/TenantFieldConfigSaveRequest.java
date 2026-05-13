package my.management.module.tenant.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenantFieldConfigSaveRequest {

    @NotBlank(message = "模块编码不能为空")
    @Size(max = 64, message = "模块编码过长")
    private String moduleCode;

    @Valid
    private List<FieldItem> fields = new ArrayList<>();

    @Data
    public static class FieldItem {

        @NotBlank(message = "字段编码不能为空")
        @Size(max = 80, message = "字段编码过长")
        private String fieldKey;

        @NotBlank(message = "字段名称不能为空")
        @Size(max = 80, message = "字段名称过长")
        private String fieldLabel;

        private Boolean visible;

        private Boolean required;

        private Integer sortNo;

        @Size(max = 30, message = "字段类型过长")
        private String fieldType;

        @Size(max = 2000, message = "字段选项配置过长")
        private String optionsJson;

        @Size(max = 300, message = "字段备注过长")
        private String remark;
    }
}
