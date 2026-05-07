package my.management.common.service;

import my.hive.common.context.TenantPermissionContext;
import my.management.common.enums.PlatformTenantEnum;
import org.springframework.stereotype.Service;
/**
 * DeveloperAccessService 属于管理端后端通用能力层，提供跨模块可复用的通用服务能力。
 */
@Service
public class DeveloperAccessService {

    public boolean isDeveloperLogin(String loginName) {
        return false;
    }

    public boolean isCurrentUserDeveloper() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        return PlatformTenantEnum.isSuper(tenantCode);
    }
}
