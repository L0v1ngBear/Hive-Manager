package my.management.module.employee.model.vo;

import lombok.Data;

/**
 * 管理端生成的小程序加入组织邀请码。
 */
@Data
public class OrganizationJoinCodeVO {

    private String joinCode;

    private String tenantCode;

    private String tenantName;

    private Integer expiresInSeconds;

    private String expireAt;
}
