package my.management.module.organization.model.vo;

import lombok.Data;

/**
 * 组织架构概览统计出参。
 */
@Data
public class OrganizationStatsVO {

    private Long departmentCount = 0L;

    private Long employeeCount = 0L;

    private Long enabledDepartmentCount = 0L;

    private Long emptyDepartmentCount = 0L;
}
