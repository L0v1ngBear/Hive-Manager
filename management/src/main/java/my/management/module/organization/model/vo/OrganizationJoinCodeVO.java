package my.management.module.organization.model.vo;

import lombok.Data;

/**
 * 组织加入码出参。
 */
@Data
public class OrganizationJoinCodeVO {

    private String organizationCode;

    private Long expireAt;

    private Long expiresInSeconds;
}
