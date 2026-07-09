package my.management.module.installation.model.enums;

import my.hive.common.exception.BusinessException;

public enum InstallationTaskStatusEnum {
    PRODUCTION_COMPLETED("production_completed"),
    SHIPPED_PENDING_INSTALL("shipped_pending_install"),
    COMPLETED_ACCEPTED("completed_accepted");

    private final String code;

    InstallationTaskStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }

    public static InstallationTaskStatusEnum require(String value) {
        for (InstallationTaskStatusEnum status : values()) {
            if (status.code.equals(value)) {
                return status;
            }
        }
        throw new BusinessException("安装任务状态不合法");
    }
}
