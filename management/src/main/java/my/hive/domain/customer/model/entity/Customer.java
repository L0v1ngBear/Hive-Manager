package my.hive.domain.customer.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * Customer 属于管理端后端客户模块，定义持久化实体结构，用于表字段映射。
 */
@TableName
@Data
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String customerName;

    private Integer customerType;

    private String constructionArea;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
