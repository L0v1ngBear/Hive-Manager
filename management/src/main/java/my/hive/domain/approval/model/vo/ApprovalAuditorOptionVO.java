package my.hive.domain.approval.model.vo;

import lombok.Data;

/**
 * 可指定审批人选项。
 */
@Data
public class ApprovalAuditorOptionVO {

    private Long id;
    private String name;
    private String empNo;
    private String departmentName;
    private String positionName;
    private Boolean defaultAuditor;
}
