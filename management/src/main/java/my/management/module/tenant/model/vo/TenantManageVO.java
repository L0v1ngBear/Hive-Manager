package my.management.module.tenant.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TenantManageVO {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private String logoUrl;

    private Integer tenantType;

    private String contactPerson;

    private String contactPhone;

    private Integer status;

    private String packageCode;

    private String packageName;

    private String subscriptionStatus;

    private LocalDateTime subscriptionStartTime;

    private LocalDateTime subscriptionEndTime;

    private Integer maxUsers;

    private Integer maxStorageMb;

    private String featureFlags;

    private List<String> enabledFeatures;

    private Long ownerUserId;

    private String ownerName;

    private String ownerLoginName;

    private String ownerPhone;

    private Integer ownerAttendanceRequired;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
