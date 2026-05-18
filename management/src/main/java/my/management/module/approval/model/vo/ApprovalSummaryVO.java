package my.management.module.approval.model.vo;

import lombok.Data;

/**
 * Approval center pending counters used by web and mini red-dot badges.
 */
@Data
public class ApprovalSummaryVO {

    private long leavePending;

    private long financePending;

    private long resignationPending;

    private long orderPending;

    private long totalPending;
}
