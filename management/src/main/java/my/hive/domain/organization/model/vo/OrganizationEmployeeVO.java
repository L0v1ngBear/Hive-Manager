package my.hive.domain.organization.model.vo;

import lombok.Data;

/**
 * 组织架构部门员工出参。
 */
@Data
public class OrganizationEmployeeVO {

    private Long id;

    private String name;

    private String empNo;

    private String phone;

    private String departmentName;

    private String positionName;

    private Integer status;
}
