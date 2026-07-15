package my.hive.domain.employee.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeePermissionNodeVO {

    private String code;
    private String name;
    private String moduleCode;
    private Integer type;
    private Integer sort;
    private Boolean assignable;
    private Boolean roleGranted;
    private List<EmployeePermissionRoleSourceVO> roleSources = new ArrayList<>();
    private String personalEffect;
    private Boolean effective;
    private String effectiveSource;
    private List<EmployeePermissionNodeVO> children = new ArrayList<>();
}
