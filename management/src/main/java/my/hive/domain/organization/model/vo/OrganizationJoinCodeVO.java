package my.hive.domain.organization.model.vo;

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
