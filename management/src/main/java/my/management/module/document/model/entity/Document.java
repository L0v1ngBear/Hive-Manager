package my.management.module.document.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * Document 属于管理端后端单据模块，定义持久化实体结构，用于表字段映射。
 */
@TableName
@Data
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("parent_id")
    private Long parentId = 0L;

    @TableField("name")
    private String name;

    @TableField("original_name")
    private String originalName;

    @TableField("type")
    private Integer type;

    @TableField("file_url")
    private String fileUrl;

    @TableField("storage_provider")
    private String storageProvider;

    @TableField("storage_bucket")
    private String storageBucket;

    @TableField("storage_object_key")
    private String storageObjectKey;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_ext")
    private String fileExt;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_hash")
    private String fileHash;

    @TableField("etag")
    private String etag;

    @TableField("upload_status")
    private String uploadStatus;

    @TableField("creator_id")
    private Long creatorId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted = 0;
}
