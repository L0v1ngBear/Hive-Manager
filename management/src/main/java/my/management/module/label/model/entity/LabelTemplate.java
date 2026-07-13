package my.management.module.label.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标签模板实体。
 * 管理端和小程序端共同使用 label_template 表，确保模板维护和打印选择保持联动。
 */
@Data
@TableName("label_template")
public class LabelTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("name")
    private String name;

    /**
     * 打印类型：label-标签，triplicate-三联单。
     */
    @TableField("print_type")
    private String printType;

    /**
     * PRN、TSPL 或 ESC/POS 原始模板内容，小程序端打印直接使用该字段。
     */
    @TableField("content")
    private String content;

    /**
     * 可视化设计器保存的 JSON 结构，打印时仍以 content 中的 TSPL/PRN 为准。
     */
    @TableField("design_json")
    private String designJson;

    @TableField("width_mm")
    private BigDecimal widthMm;

    @TableField("height_mm")
    private BigDecimal heightMm;

    /**
     * 模板中识别出的变量名，使用英文逗号分隔。
     */
    @TableField("variables")
    private String variables;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("status")
    private Integer status;

    @TableField(value = "creator_id", fill = FieldFill.INSERT)
    private Long creatorId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
