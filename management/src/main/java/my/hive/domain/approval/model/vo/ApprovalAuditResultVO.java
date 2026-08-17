package my.hive.domain.approval.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result of one approval action. A successful request does not necessarily
 * mean that a multi-auditor flow has completed.
 */
@Data
@AllArgsConstructor
public class ApprovalAuditResultVO {

    private String decision;

    private Boolean completed;

    private Integer remainingAuditorCount;

    private String message;

    public static ApprovalAuditResultVO pending(int remainingAuditorCount) {
        int remaining = Math.max(1, remainingAuditorCount);
        return new ApprovalAuditResultVO(
                "pending",
                false,
                remaining,
                "本次审批已提交，仍有 " + remaining + " 位审批人待处理");
    }

    public static ApprovalAuditResultVO approved(String subject) {
        return new ApprovalAuditResultVO("approved", true, 0, subject + "已全部通过");
    }

    public static ApprovalAuditResultVO rejected(String subject) {
        return new ApprovalAuditResultVO("rejected", true, 0, subject + "已驳回");
    }
}
