package my.hive.domain.tenant.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantLicenseUpdateRequest {

    @NotNull(message = "企业ID不能为空")
    private Long id;

    @Size(max = 50, message = "套餐编码不能超过50个字符")
    private String packageCode;

    @Size(max = 50, message = "订阅状态不能超过50个字符")
    private String subscriptionStatus;

    private LocalDateTime subscriptionStartTime;

    private LocalDateTime subscriptionEndTime;

    @Min(value = 0, message = "最大用户数不能为负数")
    private Integer maxUsers;

    @Min(value = 0, message = "存储配额不能为负数")
    private Integer maxStorageMb;

    @Size(max = 2000, message = "功能配置不能超过2000个字符")
    private String featureFlags;
}
