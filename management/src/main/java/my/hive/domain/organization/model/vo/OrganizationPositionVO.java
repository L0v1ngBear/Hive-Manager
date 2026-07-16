package my.hive.domain.organization.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganizationPositionVO {

    private Long id;

    private Long departmentId;

    private String departmentName;

    private String positionName;

    private String positionCode;

    private Integer sortNo;

    private Integer status;

    private Long employeeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
