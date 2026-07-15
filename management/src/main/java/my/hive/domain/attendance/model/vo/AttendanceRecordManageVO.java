package my.hive.domain.attendance.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理端考勤记录列表出参。
 */
@Data
public class AttendanceRecordManageVO {

    private Long id;

    private String punchId;

    private Long userId;

    private String employeeName;

    private String empNo;

    private String phone;

    private String departmentName;

    private LocalTime signInTime;

    private Integer signInStatus;

    private LocalTime signOutTime;

    private Integer signOutStatus;

    private String status;

    private String statusText;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
