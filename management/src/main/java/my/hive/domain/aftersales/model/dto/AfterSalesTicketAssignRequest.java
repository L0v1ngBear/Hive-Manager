package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AfterSalesTicketAssignRequest {
    @NotNull(message = "请选择指派人员")
    private Long assigneeUserId;
}
