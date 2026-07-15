package my.hive.domain.attendance.model.enums;

import java.util.Set;

/**
 * 考勤打卡状态。管理端和总览大盘统一从这里解析状态含义。
 */
public enum AttendancePunchStatusEnum {
    NORMAL(0, "normal", "正常"),
    LATE(1, "late", "迟到"),
    EARLY(2, "early", "早退"),
    ABSENT(3, "missing", "缺勤"),
    OVERTIME(4, "overtime", "加班"),
    LEAVE(5, "leave", "请假"),
    MISSING_CARD(6, "missing", "缺卡"),
    UNKNOWN(-1, "missing", "考勤异常");

    private static final Set<AttendancePunchStatusEnum> LEAVE_COVERABLE_SIGN_IN = Set.of(ABSENT, MISSING_CARD);
    private static final Set<AttendancePunchStatusEnum> LEAVE_COVERABLE_SIGN_OUT = Set.of(EARLY, ABSENT, MISSING_CARD);

    private final Integer code;
    private final String viewKey;
    private final String label;

    AttendancePunchStatusEnum(Integer code, String viewKey, String label) {
        this.code = code;
        this.viewKey = viewKey;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getViewKey() {
        return viewKey;
    }

    public String getLabel() {
        return label;
    }

    public static AttendancePunchStatusEnum of(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (AttendancePunchStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static boolean canBeCoveredByLeaveForSignIn(Integer status) {
        return status == null || LEAVE_COVERABLE_SIGN_IN.contains(of(status));
    }

    public static boolean canBeCoveredByLeaveForSignOut(Integer status) {
        return status == null || LEAVE_COVERABLE_SIGN_OUT.contains(of(status));
    }

    public static ResolvedStatus resolveManageStatus(Integer signInStatus, Integer signOutStatus) {
        AttendancePunchStatusEnum signIn = of(signInStatus);
        if (signInStatus != null && signIn != NORMAL) {
            return new ResolvedStatus(signIn.viewKey, signIn.label);
        }
        AttendancePunchStatusEnum signOut = of(signOutStatus);
        if (signOutStatus != null && signOut != NORMAL) {
            return new ResolvedStatus(signOut.viewKey, signOut.label);
        }
        return new ResolvedStatus(NORMAL.viewKey, NORMAL.label);
    }

    public static String resolveDashboardText(Integer signInStatus, Integer signOutStatus) {
        AttendancePunchStatusEnum signIn = of(signInStatus);
        if (signInStatus != null) {
            if (signIn == LATE) {
                return LATE.label;
            }
            if (signIn == ABSENT || signIn == MISSING_CARD) {
                return ABSENT.label;
            }
        }
        AttendancePunchStatusEnum signOut = of(signOutStatus);
        if (signOutStatus != null) {
            if (signOut == EARLY) {
                return EARLY.label;
            }
            if (signOut == ABSENT || signOut == MISSING_CARD) {
                return MISSING_CARD.label;
            }
        }
        return UNKNOWN.label;
    }

    public record ResolvedStatus(String viewKey, String label) {
    }
}
