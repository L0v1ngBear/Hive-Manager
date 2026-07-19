package my.hive.domain.organization.model;

import lombok.Data;

@Data
public class OrganizationInvitationPayload {
    private String tenantCode;
    private Long issuerUserId;
    private Long expiresAt;
    private Integer remainingUses;
}
