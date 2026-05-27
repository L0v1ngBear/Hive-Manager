package my.management.module.tenant.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantLicenseUpdateRequest {

    @NotNull(message = "tenant id is required")
    private Long id;

    @Size(max = 50, message = "package code is too long")
    private String packageCode;

    @Size(max = 50, message = "subscription status is too long")
    private String subscriptionStatus;

    private LocalDateTime subscriptionStartTime;

    private LocalDateTime subscriptionEndTime;

    @Min(value = 0, message = "max users cannot be negative")
    private Integer maxUsers;

    @Min(value = 0, message = "经营建议额度不能为负数")
    private Integer maxAiAdvicePerMonth;

    @Min(value = 0, message = "storage quota cannot be negative")
    private Integer maxStorageMb;

    @Size(max = 2000, message = "feature flags are too long")
    private String featureFlags;
}
