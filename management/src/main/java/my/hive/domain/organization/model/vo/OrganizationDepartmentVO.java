package my.hive.domain.organization.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构部门节点出参，前端按 children 渲染树形结构。
 */
@Data
public class OrganizationDepartmentVO {

    private Long id;

    private Long parentId;

    private String deptName;

    private String deptCode;

    private String leaderName;

    private Integer sortNo;

    private Integer status;

    private Long employeeCount = 0L;

    private Long positionCount = 0L;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<OrganizationDepartmentVO> children = new ArrayList<>();
}
