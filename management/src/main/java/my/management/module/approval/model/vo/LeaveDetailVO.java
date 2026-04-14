package my.management.module.approval.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaveDetailVO {

    private String leaveCode;

    private Integer leaveType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String reason;

    private Integer status;

    private String auditComment;

    private Long applyUserId;

    private String applyUserName;

    private Long auditorId;

    private String auditorName;
}
