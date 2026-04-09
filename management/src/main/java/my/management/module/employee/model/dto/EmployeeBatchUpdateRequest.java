package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EmployeeBatchUpdateRequest {

    @NotEmpty(message = "ids cannot be empty")
    private List<Long> ids;

    private Long departmentId;

    private Long positionId;

    private Long leaderId;

    private Integer status;

    private String remark;
}
