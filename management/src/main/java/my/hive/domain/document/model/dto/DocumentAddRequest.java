package my.hive.domain.document.model.dto;

import lombok.Data;
/**
 * DocumentAddRequest 属于管理端后端单据模块，定义入参结构。
 */
@Data
public class DocumentAddRequest {
    private Long parentId;
    private String name;
}
