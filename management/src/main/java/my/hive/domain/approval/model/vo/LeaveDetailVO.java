package my.hive.domain.approval.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * LeaveDetailVO 属于管理端后端审批模块，定义出参结构。
 */
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

    private String auditorIds;

    private String auditorName;
}
