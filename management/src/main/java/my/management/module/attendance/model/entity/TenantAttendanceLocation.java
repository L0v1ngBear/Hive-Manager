package my.management.module.attendance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TenantAttendanceLocation stores one allowed attendance punch location.
 */
@Data
@TableName("tenant_attendance_location")
public class TenantAttendanceLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String locationName;

    private Double latitude;

    private Double longitude;

    private String address;

    private Double radius;

    private Integer status;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
