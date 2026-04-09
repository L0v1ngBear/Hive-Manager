package my.management.module.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("emp_employee_change_log")
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
