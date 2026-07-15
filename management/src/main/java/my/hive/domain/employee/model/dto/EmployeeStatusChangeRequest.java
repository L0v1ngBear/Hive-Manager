package my.hive.domain.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * EmployeeStatusChangeRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeStatusChangeRequest {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "status is required")
    private Integer status;

    private String remark;
}
