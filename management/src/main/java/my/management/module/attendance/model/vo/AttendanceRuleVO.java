package my.management.module.attendance.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 考勤规则出参，供管理端配置抽屉回显。
 */
@Data
public class AttendanceRuleVO {

    private String workStartTime;

    private String workEndTime;

    private String offWorkStartTime;

    private String offWorkEndTime;

    private String overTimeStartTime;

    private String overTimeEndTime;

    private Integer lateToleranceMinutes = 0;

    private Integer earlyToleranceMinutes = 0;

    private List<Integer> workDays = new ArrayList<>();

    private Boolean enableGps = Boolean.TRUE;

    private Double latitude;

    private Double longitude;

    private Double radius;

    private String address;

    private List<AttendanceLocationVO> locations = new ArrayList<>();

    private Boolean enableWifi = Boolean.FALSE;

    private String wifiSsid;
}
