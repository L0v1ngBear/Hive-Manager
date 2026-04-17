package my.management.module.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * EmployeeChangeLog 属于管理端后端员工模块，定义持久化实体结构，用于表字段映射。
 */
@TableName
@Data
public class EmployeeChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long employeeId;

    private String changeType;

    private String beforeJson;

    private String afterJson;

    private Long operatorUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
