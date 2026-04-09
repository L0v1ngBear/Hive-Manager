package my.management.module.employee.model.vo;

import lombok.Data;

@Data
public class EmployeeStatsVO {

    private Long totalEmployees;
    private Double todayAttendanceRate;
    private Long departmentCount;
    private Long pendingOnboardCount;
}
