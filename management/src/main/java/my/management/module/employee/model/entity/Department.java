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
 * Department 属于管理端后端员工模块，定义持久化实体结构，用于表字段映射。
 */
@TableName("emp_department")
@Data
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String deptName;

    private String deptCode;

    /**
     * 上级部门 ID，为空表示一级部门。
     */
    private Long parentId;

    /**
     * 部门负责人名称，先用文本保存，避免强绑定用户 ID 导致历史负责人展示丢失。
     */
    private String leaderName;

    private Integer sortNo;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
