package my.management.module.label.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签模板出参。
 */
@Data
public class LabelTemplateVO {

    private Long id;

    private String name;

    private String printType;

    private String content;

    private String designJson;

    private BigDecimal widthMm;

    private BigDecimal heightMm;

    private List<String> variables;

    private String fileName;

    private Long fileSize;

    private Integer isDefault;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
