package my.hive.domain.approval.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaveSubmitRequest {

    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    @NotNull(message = "请假开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "请假结束时间不能为空")
    private LocalDateTime endTime;

    @NotBlank(message = "请假事由不能为空")
    private String reason;

    private Long auditorId;

    private List<Long> auditorIds;
}
