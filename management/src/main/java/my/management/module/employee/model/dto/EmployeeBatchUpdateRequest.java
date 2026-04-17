package my.management.module.employee.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
/**
 * EmployeeBatchUpdateRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeBatchUpdateRequest {

    @NotEmpty(message = "ids cannot be empty")
    private List<Long> ids;

    private Long departmentId;

    private Long positionId;

    private String leaderName;

    private Integer status;

    private String remark;
}
