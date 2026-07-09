package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.tenant.model.enums.TenantPlanEnum;
import my.management.module.tenant.model.enums.TenantSubscriptionStatusEnum;
import my.management.module.tenant.model.vo.TenantFeatureOptionVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TenantLicenseService {

    private static final int MAX_FEATURE_FLAGS_LENGTH = 2000;

    private static final Set<String> ALLOWED_STATUSES = TenantSubscriptionStatusEnum.allowedCodes();
    private static final Set<String> ALLOWED_PLANS = TenantPlanEnum.allowedCodes();
    private static final Set<String> BASE_MODULE_FEATURES = Set.copyOf(TenantFeatureEnum.baseModuleCodes());
    private static final Pattern FEATURE_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.:\\-]{0,100}$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    public void applyDefaultTrial(Tenant tenant) {
        if (tenant == null) {
            return;
        }
        TenantPlanEnum plan = TenantPlanEnum.TRIAL;
        tenant.setPackageCode(plan.getCode());
        tenant.setPackageName(plan.getLabel());
        tenant.setSubscriptionStatus(TenantSubscriptionStatusEnum.TRIAL.getCode());
        tenant.setSubscriptionStartTime(LocalDateTime.now());
        tenant.setSubscriptionEndTime(LocalDateTime.now().plusDays(30));
        tenant.setMaxUsers(plan.getMaxUsers());
        tenant.setMaxStorageMb(plan.getStorageQuotaMb());
        tenant.setFeatureFlags(defaultFeatureFlags(plan.getCode()));
    }

    public void applyLicenseUpdate(Tenant tenant, TenantLicenseUpdateRequest request) {
        if (tenant == null || request == null) {
            throw new BusinessException("租户授权参数不能为空");
        }
        String planCode = normalizePlanCode(request.getPackageCode(), defaultText(tenant.getPackageCode(), TenantPlanEnum.TRIAL.getCode()));
        TenantPlanEnum plan = TenantPlanEnum.of(planCode);

        tenant.setPackageCode(planCode);
        tenant.setPackageName(plan.getLabel());
        tenant.setSubscriptionStatus(normalizeSubscriptionStatus(request.getSubscriptionStatus(), planCode, request.getSubscriptionEndTime()));
        tenant.setSubscriptionStartTime(request.getSubscriptionStartTime() == null
                ? defaultStartTime(tenant.getSubscriptionStartTime())
                : request.getSubscriptionStartTime());
        tenant.setSubscriptionEndTime(request.getSubscriptionEndTime());
        tenant.setMaxUsers(resolveLimit(request.getMaxUsers(), plan.getMaxUsers(), 9999));
        tenant.setMaxStorageMb(resolveLimit(request.getMaxStorageMb(), plan.getStorageQuotaMb(), 102400));
        tenant.setFeatureFlags(normalizeFeatureFlags(request.getFeatureFlags(), planCode));
    }

    public void ensureTenantUsable(String tenantCode) {
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant)) {
            throw new BusinessException(403, "租户已到期或被停用，请联系平台管理员续费");
        }
    }

    public void ensureUserQuotaAvailable(String tenantCode) {
        Tenant tenant = requireTenant(tenantCode);
        Integer maxUsers = tenant.getMaxUsers();
        if (maxUsers == null) {
            return;
        }
        if (maxUsers <= 0) {
            throw new BusinessException("当前套餐暂未开放员工账号，请升级套餐后再新增员工");
        }
        Long usedUsers = employeeMapper.countAvailableEmployees(tenantCode);
        if (usedUsers != null && usedUsers >= maxUsers) {
            throw new BusinessException("当前套餐最多允许 " + maxUsers + " 名员工，请升级套餐后再新增员工");
        }
    }

    public void requireFeatureEnabled(String tenantCode, String featureName, String message) {
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant) || !isFeatureEnabled(tenant, featureName)) {
            throw new BusinessException(403, StringUtils.hasText(message) ? message : "当前套餐暂未开放该功能，请联系平台管理员开通");
        }
    }

    public boolean isFeatureEnabled(String tenantCode, String featureName) {
        if (!StringUtils.hasText(featureName)) {
            return false;
        }
        Tenant tenant = requireTenant(tenantCode);
        return isTenantUsable(tenant) && isFeatureEnabled(tenant, featureName);
    }

    public List<String> enabledFeatureKeys(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            return List.of();
        }
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant)) {
            return List.of();
        }
        return List.copyOf(buildFeatureKeys(tenant, false));
    }

    public List<TenantFeatureOptionVO> featureCatalog() {
        List<TenantFeatureOptionVO> features = new ArrayList<>();
        for (TenantFeatureEnum item : TenantFeatureEnum.values()) {
            features.add(feature(item));
        }
        return features;
    }

    private TenantFeatureOptionVO feature(TenantFeatureEnum feature) {
        return new TenantFeatureOptionVO(
                feature.getCode(),
                feature.getName(),
                feature.getCategory(),
                feature.getDescription(),
                feature.isBaseModule(),
                feature.isDefaultEnabled()
        );
    }

    public void clearTenantRuntimeCache(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            return;
        }
        try {
            stringRedisTemplate.delete(List.of(
                    redisKeyBuilder.cache("tenant", "status", tenantCode),
                    redisKeyBuilder.cache("tenant", "license", tenantCode),
                    redisKeyBuilder.cache("tenant", "features", tenantCode),
                    redisKeyBuilder.cache("tenant", "attendance-rule", tenantCode)
            ));
        } catch (Exception ignored) {
        }
    }

    public boolean isTenantUsable(Tenant tenant) {
        if (tenant == null || DeleteFlagEnum.isDeleted(tenant.getDeleted()) || !CommonStatusEnum.isEnabled(tenant.getStatus())) {
            return false;
        }
        String subscriptionStatus = normalizeStatusOnly(tenant.getSubscriptionStatus());
        if (TenantSubscriptionStatusEnum.isUnavailable(subscriptionStatus)) {
            return false;
        }
        LocalDateTime endTime = tenant.getSubscriptionEndTime();
        return endTime == null || !endTime.isBefore(LocalDateTime.now());
    }

    private Tenant requireTenant(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException(401, "登录状态异常，请重新登录");
        }
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, tenantCode.trim())
                .last("LIMIT 1"));
        if (tenant == null) {
            throw new BusinessException(403, "租户不存在或已被停用");
        }
        return tenant;
    }

    private String normalizePlanCode(String value, String fallback) {
        String normalized = defaultText(value, fallback).trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PLANS.contains(normalized)) {
            throw new BusinessException("不支持的套餐类型：" + value);
        }
        return normalized;
    }

    private String normalizeSubscriptionStatus(String value, String planCode, LocalDateTime endTime) {
        if (StringUtils.hasText(value)) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_STATUSES.contains(normalized)) {
                throw new BusinessException("不支持的授权状态：" + value);
            }
            return normalized;
        }
        if (endTime != null && endTime.isBefore(LocalDateTime.now())) {
            return TenantSubscriptionStatusEnum.EXPIRED.getCode();
        }
        return TenantPlanEnum.TRIAL.getCode().equals(planCode)
                ? TenantSubscriptionStatusEnum.TRIAL.getCode()
                : TenantSubscriptionStatusEnum.ACTIVE.getCode();
    }

    private String normalizeStatusOnly(String value) {
        if (!StringUtils.hasText(value)) {
            return TenantSubscriptionStatusEnum.ACTIVE.getCode();
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Integer resolveLimit(Integer requested, Integer defaultValue, int hardMax) {
        int value = requested == null ? defaultValue : requested;
        if (value < 0) {
            throw new BusinessException("套餐额度不能小于 0");
        }
        return Math.min(value, hardMax);
    }

    private String normalizeFeatureFlags(String value, String planCode) {
        if (!StringUtils.hasText(value)) {
            return defaultFeatureFlags(planCode);
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_FEATURE_FLAGS_LENGTH) {
            throw new BusinessException("功能开关配置过长");
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            if (!root.isObject()) {
                throw new BusinessException("功能开关配置格式不正确");
            }
            buildFeatureKeys(defaultText(planCode, TenantPlanEnum.TRIAL.getCode()), trimmed, true, null);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("功能开关配置格式不正确");
        }
        return trimmed;
    }

    private String defaultFeatureFlags(String planCode) {
        return "{"
                + "\"modules\":{"
                + "\"dashboard\":true,"
                + "\"order\":true,"
                + "\"inventory\":true,"
                + "\"badProduct\":true,"
                + "\"customer\":true,"
                + "\"price\":true,"
                + "\"receipt\":true,"
                + "\"approval\":true,"
                + "\"attendance\":true,"
                + "\"employee\":true,"
                + "\"role\":true,"
                + "\"label\":true,"
                + "\"document\":true,"
                + "\"equipment\":true,"
                + "\"manual\":true"
                + "},"
                + "\"custom\":{}"
                + "}";
    }

    private boolean isFeatureEnabled(Tenant tenant, String featureName) {
        if (tenant == null || !StringUtils.hasText(featureName)) {
            return false;
        }
        String normalized = normalizeFeatureKey(featureName, false);
        return StringUtils.hasText(normalized) && buildFeatureKeys(tenant, false).contains(normalized);
    }

    private Set<String> buildFeatureKeys(Tenant tenant, boolean strict) {
        String planCode = defaultText(tenant.getPackageCode(), TenantPlanEnum.TRIAL.getCode());
        String featureFlags = tenant.getFeatureFlags();
        return buildFeatureKeys(planCode, featureFlags, strict, tenant.getTenantCode());
    }

    private Set<String> buildFeatureKeys(String planCode, String featureFlags, boolean strict, String tenantCode) {
        LinkedHashSet<String> enabled = new LinkedHashSet<>(baseFeatureKeys(planCode));
        String flags = StringUtils.hasText(featureFlags) ? featureFlags : defaultFeatureFlags(planCode);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(flags);
            if (!root.isObject()) {
                if (strict) {
                    throw new BusinessException("功能开关配置格式不正确");
                }
                return Collections.unmodifiableSet(enabled);
            }
            applyFeatureNode(root, "", enabled, strict);
        } catch (BusinessException ex) {
            if (strict) {
                throw ex;
            }
            log.warn("invalid tenant feature flags, tenantCode={}", tenantCode, ex);
        } catch (Exception ex) {
            if (strict) {
                throw new BusinessException("功能开关解析失败");
            }
            log.warn("invalid tenant feature flags, tenantCode={}", tenantCode, ex);
        }
        return Collections.unmodifiableSet(enabled);
    }

    private Set<String> baseFeatureKeys(String planCode) {
        return new LinkedHashSet<>(BASE_MODULE_FEATURES);
    }

    private void applyFeatureNode(JsonNode node, String path, Set<String> enabled, boolean strict) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isBoolean()) {
            applyFeatureValue(path, node.asBoolean(), enabled, strict);
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item != null && item.isTextual()) {
                    applyFeatureValue(item.asText(), true, enabled, strict);
                } else if (strict) {
                    throw new BusinessException("功能开关数组仅支持字符串功能码");
                }
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String childPath = path.isBlank() ? field.getKey() : path + "." + field.getKey();
            applyFeatureNode(field.getValue(), childPath, enabled, strict);
        }
    }

    private void applyFeatureValue(String rawFeatureKey, boolean enabledFlag, Set<String> enabled, boolean strict) {
        String featureKey = normalizeFeatureKey(rawFeatureKey, strict);
        if (!StringUtils.hasText(featureKey)) {
            return;
        }
        if (enabledFlag) {
            enabled.add(featureKey);
        } else {
            enabled.remove(featureKey);
        }
    }

    private String normalizeFeatureKey(String rawFeatureKey, boolean strict) {
        if (!StringUtils.hasText(rawFeatureKey)) {
            if (strict) {
                throw new BusinessException("功能码不能为空");
            }
            return null;
        }
        String featureKey = rawFeatureKey.trim();
        if (featureKey.startsWith("modules.")) {
            featureKey = "module." + featureKey.substring("modules.".length());
        }
        if (featureKey.equals("modules")) {
            featureKey = "module";
        }
        if (featureKey.startsWith("enabledFeatures.") || featureKey.startsWith("features.")) {
            return null;
        }
        if ("module".equals(featureKey) || "custom".equals(featureKey) || featureKey.startsWith("platform.")) {
            if (strict) {
                throw new BusinessException("非法租户功能码：" + rawFeatureKey);
            }
            return null;
        }
        if (!FEATURE_KEY_PATTERN.matcher(featureKey).matches()) {
            if (strict) {
                throw new BusinessException("非法功能码：" + rawFeatureKey);
            }
            return null;
        }
        return featureKey;
    }

    private LocalDateTime defaultStartTime(LocalDateTime existed) {
        return existed == null ? LocalDateTime.now() : existed;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

}
