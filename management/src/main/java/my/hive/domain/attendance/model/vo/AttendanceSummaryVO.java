package my.hive.domain.attendance.model.vo;

import lombok.Data;

/**
 * 管理端考勤统计卡片出参。
 */
@Data
public class AttendanceSummaryVO {

    private Long totalEmployeeCount = 0L;

    private Long actualCount = 0L;

    private Long lateCount = 0L;

    private Long earlyCount = 0L;

    private Long missingCount = 0L;

    private Double attendanceRate = 0D;
}
