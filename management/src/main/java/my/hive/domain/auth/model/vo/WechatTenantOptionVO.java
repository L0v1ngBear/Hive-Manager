package my.hive.domain.auth.model.vo;

import lombok.Data;

@Data
public class WechatTenantOptionVO {
    private String tenantCode;
    private String tenantName;
    private String tenantLogoUrl;
}
