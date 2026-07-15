package my.hive.domain.tenant.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantFieldConfigVO {

    private Long id;

    private String tenantCode;

    private String moduleCode;

    private String fieldKey;

    private String fieldLabel;

    private Boolean visible;

    private Boolean required;

    private Integer sortNo;

    private String fieldType;

    private Boolean custom;

    private String optionsJson;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
