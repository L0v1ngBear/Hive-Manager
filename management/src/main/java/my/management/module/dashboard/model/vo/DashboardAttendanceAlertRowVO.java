package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardAttendanceAlertRowVO {

    private Long userId;

    private Integer signInStatus;

    private Integer signOutStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
