package my.hive.domain.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * EmployeeExt 属于管理端后端员工模块，定义持久化实体结构，用于表字段映射。
 */
@TableName("emp_employee_ext")
@Data
public class EmployeeExt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tenantCode;

    private String empNo;

    private String email;

    private String employeeType;

    private LocalDate entryDate;

    private String avatarUrl;

    private String remark;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
