package my.management.module.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("user")
@Data
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String name;

    private String loginName;

    private String phone;

    @TableField(select = false)
    private String password;

    private String departmentName;

    private String position;

    private Long managerId;

    private Integer status;

    private Integer roleLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
