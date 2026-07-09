package my.management.module.employee.model.vo;

import lombok.Data;

import java.util.Set;

@Data
public class EmployeePermissionOverrideVO {

    private Long userId;

    private Set<Long> rolePermissionIds;

    private Set<Long> grantPermissionIds;

    private Set<Long> denyPermissionIds;
}
