package my.management.module.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * Position 属于管理端后端员工模块，定义持久化实体结构，用于表字段映射。
 */
@TableName("emp_position")
@Data
public class Position {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String positionName;

    private String positionCode;

    private Long departmentId;

    private Integer sortNo;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
