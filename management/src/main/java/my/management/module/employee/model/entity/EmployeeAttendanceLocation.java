package my.management.module.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("employee_attendance_location")
public class EmployeeAttendanceLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long userId;

    private Long attendanceLocationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
