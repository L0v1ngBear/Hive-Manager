package my.management.module.document.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * DocumentVO 属于管理端后端单据模块，定义出参结构。
 */
@Data
public class DocumentVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer type;
    private String fileUrl;
    private Long fileSize;
    private String fileExt;
    private LocalDateTime createTime;
}
