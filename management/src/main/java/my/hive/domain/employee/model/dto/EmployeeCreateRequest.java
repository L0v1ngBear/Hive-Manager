package my.hive.domain.employee.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
/**
 * EmployeeCreateRequest 属于管理端后端员工模块，定义入参结构。
 */
@Data
public class EmployeeCreateRequest {

    @NotBlank(message = "员工姓名不能为空")
    private String name;

    @NotBlank(message = "员工手机号不能为空")
    private String phone;

    private String email;

    private String employeeType;

    @NotNull(message = "部门ID不能为空")
    private Long departmentId;

    @NotNull(message = "职位ID不能为空")
    private Long positionId;

    private String leaderName;

    @NotNull(message = "入职日期不能为空")
    private LocalDate entryDate;

    @NotNull(message = "员工状态不能为空")
    private Integer status;

    private Integer attendanceRequired = 1;

    private List<Long> attendanceLocationIds;

    private String remark;

    private List<Long> roleIds;
}
