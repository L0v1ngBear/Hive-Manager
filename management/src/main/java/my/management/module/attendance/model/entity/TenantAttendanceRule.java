package my.management.module.attendance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

/**
 * 租户考勤规则实体，对应小程序打卡使用的 tenant_attendance_rule 表。
 */
@Data
@TableName("tenant_attendance_rule")
public class TenantAttendanceRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String tenantName;

    private Integer status;

    private Double latitude;

    private Double longitude;

    private String address;

    private Double radius;

    private LocalTime workStartTime;

    private LocalTime workEndTime;

    private LocalTime offWorkStartTime;

    private LocalTime offWorkEndTime;

    private LocalTime overTimeStartTime;

    private LocalTime overTimeEndTime;

    private Integer lateToleranceMinutes;

    private Integer earlyToleranceMinutes;

    private String workDays;

    private Integer enableGps;

    private Integer enableWifi;

    private String wifiSsid;
}
