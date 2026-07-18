package my.hive.domain.employee.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * EmployeeStatusChangeRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeStatusChangeRequest {

    @NotNull(message = "员工ID不能为空")
    private Long id;

    @NotNull(message = "员工状态不能为空")
    private Integer status;

    private String remark;
}
