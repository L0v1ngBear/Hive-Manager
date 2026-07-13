package my.management.module.label.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签模板可用变量。
 * 管理端设计器使用这份清单渲染字段面板，打印数据也应按这些字段名提供值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplateVariableVO {

    private String label;

    private String field;

    private String type;

    private String sampleValue;
}
