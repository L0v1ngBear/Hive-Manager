package my.hive.domain.dashboard.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * DashboardAttendanceAlertRowVO 属于管理端后端总览大盘模块，定义出参结构。
 */
@Data
public class DashboardAttendanceAlertRowVO {

    private Long userId;

    private String userName;

    private String departmentName;

    private Integer signInStatus;

    private Integer signOutStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
