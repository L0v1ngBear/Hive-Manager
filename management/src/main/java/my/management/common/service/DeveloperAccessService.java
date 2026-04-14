package my.management.common.service;

import jakarta.annotation.Resource;
import my.management.common.context.TenantPermissionContext;
import my.management.module.auth.mapper.AuthMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeveloperAccessService {

    private final Set<String> developerLogins;

    @Resource
    private AuthMapper authMapper;

    public DeveloperAccessService(@Value("${platform.developer-logins:platform_admin}") String developerLogins) {
        this.developerLogins = Arrays.stream(developerLogins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isDeveloperLogin(String loginName) {
        return loginName != null && developerLogins.contains(loginName.trim());
    }

    public boolean isCurrentUserDeveloper() {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (userId == null) {
            return false;
        }
        if (tenantCode == null || tenantCode.isBlank()) {
            return false;
        }
        String loginName = authMapper.selectLoginNameByUserId(userId, tenantCode);
        return isDeveloperLogin(loginName);
    }
}
