package my.management.module.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TenantFieldConfig stores tenant-level field customizations without changing business table schemas.
 */
@TableName("tenant_field_config")
@Data
public class TenantFieldConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String moduleCode;

    private String fieldKey;

    private String fieldLabel;

    private Integer visibleFlag;

    private Integer requiredFlag;

    private Integer sortNo;

    @TableField("field_type")
    private String fieldType;

    private String optionsJson;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
