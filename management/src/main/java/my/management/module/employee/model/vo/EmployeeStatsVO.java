package my.management.module.employee.model.vo;

import lombok.Data;
/**
 * EmployeeStatsVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
public class EmployeeStatsVO {

    private Long totalEmployees;
    private Double todayAttendanceRate;
    private Long departmentCount;
    private Long pendingOnboardCount;
}
