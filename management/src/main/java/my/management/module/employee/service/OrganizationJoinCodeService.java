package my.management.module.employee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.common.enums.PlatformTenantEnum;
import my.management.module.employee.model.vo.OrganizationJoinCodeVO;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.entity.Tenant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class OrganizationJoinCodeService {

    private static final Duration JOIN_CODE_TTL = Duration.ofMinutes(15);
    private static final int JOIN_CODE_EXPIRES_IN_SECONDS = (int) JOIN_CODE_TTL.toSeconds();
    private static final int MAX_GENERATE_RETRY = 8;
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final DateTimeFormatter EXPIRE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SecureRandom secureRandom = new SecureRandom();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TenantMapper tenantMapper;

    public OrganizationJoinCodeVO generateCurrentTenantJoinCode() {
        String tenantCode = normalizeTenantCode(TenantPermissionContext.getTenantCode());
        if (tenantCode == null || PlatformTenantEnum.isSuper(tenantCode)) {
            throw new BusinessException("请选择具体租户后再生成组织邀请码");
        }

        Tenant tenant = tenantMapper.selectByTenantCode(tenantCode);
        if (!isTenantUsable(tenant)) {
            throw new BusinessException("当前组织不存在或暂不可用，不能生成邀请码");
        }

        LocalDateTime expireAt = LocalDateTime.now().plus(JOIN_CODE_TTL);
        String joinCode = generateUniqueCode(tenant, expireAt);

        OrganizationJoinCodeVO vo = new OrganizationJoinCodeVO();
        vo.setJoinCode(joinCode);
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getTenantName());
        vo.setExpiresInSeconds(JOIN_CODE_EXPIRES_IN_SECONDS);
        vo.setExpireAt(expireAt.format(EXPIRE_TIME_FORMATTER));
        return vo;
    }

    private String generateUniqueCode(Tenant tenant, LocalDateTime expireAt) {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String joinCode = randomCode();
            try {
                Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(
                        joinCodeKey(joinCode),
                        buildPayload(tenant, expireAt),
                        JOIN_CODE_TTL
                );
                if (Boolean.TRUE.equals(created)) {
                    return joinCode;
                }
            } catch (Exception e) {
                throw new BusinessException("组织邀请码服务暂不可用，请稍后重试");
            }
        }
        throw new BusinessException("组织邀请码生成失败，请稍后重试");
    }

    private String buildPayload(Tenant tenant, LocalDateTime expireAt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tenantCode", tenant.getTenantCode());
            payload.put("tenantName", tenant.getTenantName());
            payload.put("createdBy", TenantPermissionContext.getUserId());
            payload.put("expireAt", expireAt.format(EXPIRE_TIME_FORMATTER));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException("组织邀请码生成失败，请稍后重试");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String joinCodeKey(String joinCode) {
        return redisKeyBuilder.cache("tenant", "join-code", joinCode);
    }

    private boolean isTenantUsable(Tenant tenant) {
        if (tenant == null || Objects.equals(tenant.getDeleted(), 1) || !Objects.equals(tenant.getStatus(), 1)) {
            return false;
        }
        String subscriptionStatus = tenant.getSubscriptionStatus();
        if (subscriptionStatus != null && !subscriptionStatus.isBlank()) {
            String normalized = subscriptionStatus.trim().toUpperCase(Locale.ROOT);
            if ("EXPIRED".equals(normalized) || "SUSPENDED".equals(normalized)) {
                return false;
            }
        }
        LocalDateTime endTime = tenant.getSubscriptionEndTime();
        return endTime == null || !endTime.isBefore(LocalDateTime.now());
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            return null;
        }
        return tenantCode.trim();
    }
}
