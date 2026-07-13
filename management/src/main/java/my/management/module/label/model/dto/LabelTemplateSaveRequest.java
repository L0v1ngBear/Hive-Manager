package my.management.module.label.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 标签模板保存入参。
 */
@Data
public class LabelTemplateSaveRequest {

    /**
     * 有 id 时更新原模板；为空时新增模板。
     */
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    /**
     * 打印类型：label-标签，triplicate-三联单。
     */
    private String printType = "label";

    @NotBlank(message = "模板内容不能为空")
    private String content;

    /**
     * 可视化设计器 JSON。源码模式可以为空。
     */
    private String designJson;

    private BigDecimal widthMm;

    private BigDecimal heightMm;

    private Integer isDefault = 0;
}
