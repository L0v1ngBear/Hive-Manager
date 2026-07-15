package my.hive.domain.employee.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class EmployeePermissionProfileVO {

    private Long userId;
    private Long permissionVersion;
    private List<EmployeePermissionRoleSourceVO> roles = new ArrayList<>();
    private Set<String> grants = new LinkedHashSet<>();
    private Set<String> denies = new LinkedHashSet<>();
    private List<EmployeePermissionNodeVO> permissions = new ArrayList<>();
}
