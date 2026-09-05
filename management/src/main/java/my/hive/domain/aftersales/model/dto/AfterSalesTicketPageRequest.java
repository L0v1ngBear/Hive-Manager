package my.hive.domain.aftersales.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AfterSalesTicketPageRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String status;
    private String ticketType;
    /** 被指派处理人的用户 ID。 */
    private Long assigneeUserId;
    /** 仅查询尚待处理的工单，排除已结案和已取消状态。 */
    private Boolean openTasksOnly;
    /** 工单创建日期起（含）。 */
    private LocalDate createdStartDate;
    /** 工单创建日期止（含）。 */
    private LocalDate createdEndDate;
}
