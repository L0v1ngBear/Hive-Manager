package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
/**
 * DashboardOverviewVO 属于管理端后端总览大盘模块，定义出参结构。
 */
@Data
public class DashboardOverviewVO {

    private Summary summary = new Summary();

    private Visibility visibility = new Visibility();

    private List<String> trendDates = new ArrayList<>();

    private List<BigDecimal> trendInMeters = new ArrayList<>();

    private List<BigDecimal> trendOutMeters = new ArrayList<>();

    private List<AlertItem> businessAlerts = new ArrayList<>();

    private List<AttendanceAlert> attendanceAlerts = new ArrayList<>();

    private AttendanceSummary attendanceSummary = new AttendanceSummary();

    private List<QuickAction> quickActions = new ArrayList<>();

    @Data
    public static class Summary {
        private Long monthOrderCount = 0L;
        private BigDecimal totalInventoryMeters = BigDecimal.ZERO;
        private Long pendingApprovalCount = 0L;
        private Long pendingPrintCount = 0L;
        private Long inventoryWarningCount = 0L;
    }

    @Data
    public static class Visibility {
        private Boolean orderVisible = Boolean.FALSE;
        private Boolean inventoryVisible = Boolean.FALSE;
        private Boolean approvalVisible = Boolean.FALSE;
        private Boolean receiptVisible = Boolean.FALSE;
        private Boolean trendVisible = Boolean.FALSE;
        private Boolean attendanceVisible = Boolean.FALSE;
        private Boolean aiAdviceVisible = Boolean.FALSE;
    }

    @Data
    public static class AlertItem {
        private String type;
        private String title;
        private String content;
        private String time;
        private String level;
    }

    @Data
    public static class AttendanceAlert {
        private Long userId;
        private String userName;
        private String departmentName;
        private String statusText;
        private String time;
    }

    @Data
    public static class AttendanceSummary {
        private Long totalEmployeeCount = 0L;
        private Long actualCount = 0L;
        private Long abnormalCount = 0L;
        private String statusText = "暂无考勤数据";
        private String statusType = "empty";
    }

    @Data
    public static class QuickAction {
        private String title;
        private String description;
        private String route;
        private String icon;
    }
}
