package my.hive.domain.attendance.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 考勤规则保存入参，管理端保存后供小程序打卡实时使用。
 */
@Data
public class AttendanceRuleSaveRequest {

    @NotBlank(message = "第一次上班时间不能为空")
    private String workStartTime;

    @NotBlank(message = "第一次下班时间不能为空")
    private String workEndTime;

    @NotBlank(message = "第二次上班时间不能为空")
    private String offWorkStartTime;

    @NotBlank(message = "第二次下班时间不能为空")
    private String offWorkEndTime;

    private String overTimeStartTime;

    private String overTimeEndTime;

    private Integer lateToleranceMinutes = 0;

    private Integer earlyToleranceMinutes = 0;

    private List<Integer> workDays;

    @NotNull(message = "请设置是否启用GPS围栏")
    private Boolean enableGps;

    private Double latitude;

    private Double longitude;

    private Double radius;

    private String address;

    private List<AttendanceLocationSaveRequest> locations;

    private Boolean enableWifi = Boolean.FALSE;

    private String wifiSsid;
}
