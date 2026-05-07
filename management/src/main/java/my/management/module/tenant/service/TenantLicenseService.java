package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.exception.BusinessException;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.mapper.TenantUsageMeterMapper;
import my.management.module.tenant.model.dto.TenantLicenseUpdateRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.entity.TenantUsageMeter;
import my.management.module.tenant.model.vo.TenantFeatureOptionVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
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

    public static final String PLAN_TRIAL = "TRIAL";
    public static final String PLAN_STARTER = "STARTER";
    public static final String PLAN_STANDARD = "STANDARD";
    public static final String PLAN_PROFESSIONAL = "PROFESSIONAL";
    public static final String PLAN_PRIVATE = "PRIVATE";

    private static final String STATUS_TRIAL = "TRIAL";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String SUPER_TENANT_CODE = "super";
    private static final String FEATURE_AI_ADVICE = "aiAdvice";
    private static final String FEATURE_ADVANCED_AI = "advancedAi";
    private static final String FEATURE_PLATFORM_SUPER = "platform.super";
    private static final String METER_AI_ADVICE = "AI_ADVICE";
    private static final int MAX_FEATURE_FLAGS_LENGTH = 2000;

    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_TRIAL, STATUS_ACTIVE, STATUS_EXPIRED, STATUS_SUSPENDED);
    private static final Set<String> ALLOWED_PLANS = Set.of(PLAN_TRIAL, PLAN_STARTER, PLAN_STANDARD, PLAN_PROFESSIONAL, PLAN_PRIVATE);
    private static final Set<String> BASE_MODULE_FEATURES = Set.of(
            "module.dashboard",
            "module.order",
            "module.inventory",
            "module.badProduct",
            "module.customer",
            "module.price",
            "module.receipt",
            "module.approval",
            "module.attendance",
            "module.employee",
            "module.role",
            "module.label",
            "module.document",
            "module.manual"
    );
    private static final Pattern FEATURE_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.:\\-]{0,100}$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, PlanLimit> PLAN_LIMITS = Map.of(
            PLAN_TRIAL, new PlanLimit("试用版", 5, 30, 512),
            PLAN_STARTER, new PlanLimit("入门版", 10, 80, 1024),
            PLAN_STANDARD, new PlanLimit("标准版", 30, 300, 5120),
            PLAN_PROFESSIONAL, new PlanLimit("专业版", 80, 1000, 20480),
            PLAN_PRIVATE, new PlanLimit("私有部署版", 9999, 100000, 102400)
    );

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantUsageMeterMapper tenantUsageMeterMapper;

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
        PlanLimit limit = PLAN_LIMITS.get(PLAN_TRIAL);
        tenant.setPackageCode(PLAN_TRIAL);
        tenant.setPackageName(limit.name());
        tenant.setSubscriptionStatus(STATUS_TRIAL);
        tenant.setSubscriptionStartTime(LocalDateTime.now());
        tenant.setSubscriptionEndTime(LocalDateTime.now().plusDays(30));
        tenant.setMaxUsers(limit.maxUsers());
        tenant.setMaxAiAdvicePerMonth(limit.maxAiAdvicePerMonth());
        tenant.setMaxStorageMb(limit.maxStorageMb());
        tenant.setFeatureFlags(defaultFeatureFlags(PLAN_TRIAL));
    }

    public void applyLicenseUpdate(Tenant tenant, TenantLicenseUpdateRequest request) {
        if (tenant == null || request == null) {
            throw new BusinessException("租户授权参数不能为空");
        }
        String planCode = normalizePlanCode(request.getPackageCode(), defaultText(tenant.getPackageCode(), PLAN_TRIAL));
        PlanLimit limit = PLAN_LIMITS.get(planCode);

        tenant.setPackageCode(planCode);
        tenant.setPackageName(limit.name());
        tenant.setSubscriptionStatus(normalizeSubscriptionStatus(request.getSubscriptionStatus(), planCode, request.getSubscriptionEndTime()));
        tenant.setSubscriptionStartTime(request.getSubscriptionStartTime() == null
                ? defaultStartTime(tenant.getSubscriptionStartTime())
                : request.getSubscriptionStartTime());
        tenant.setSubscriptionEndTime(request.getSubscriptionEndTime());
        tenant.setMaxUsers(resolveLimit(request.getMaxUsers(), limit.maxUsers(), 9999));
        tenant.setMaxAiAdvicePerMonth(resolveLimit(request.getMaxAiAdvicePerMonth(), limit.maxAiAdvicePerMonth(), 100000));
        tenant.setMaxStorageMb(resolveLimit(request.getMaxStorageMb(), limit.maxStorageMb(), 102400));
        tenant.setFeatureFlags(normalizeFeatureFlags(request.getFeatureFlags(), planCode));
    }

    public void ensureTenantUsable(String tenantCode) {
        if (isSuperTenant(tenantCode)) {
            return;
        }
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant)) {
            throw new BusinessException(403, "租户已到期或被停用，请联系平台管理员续费");
        }
    }

    public void ensureUserQuotaAvailable(String tenantCode) {
        if (isSuperTenant(tenantCode)) {
            return;
        }
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
        if (isSuperTenant(tenantCode)) {
            return;
        }
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant) || !isFeatureEnabled(tenant, featureName)) {
            throw new BusinessException(403, StringUtils.hasText(message) ? message : "当前套餐暂未开放该功能，请联系平台管理员开通");
        }
    }

    public boolean isFeatureEnabled(String tenantCode, String featureName) {
        if (isSuperTenant(tenantCode)) {
            return true;
        }
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
        if (isSuperTenant(tenantCode)) {
            LinkedHashSet<String> superFeatures = new LinkedHashSet<>(BASE_MODULE_FEATURES);
            superFeatures.add(FEATURE_AI_ADVICE);
            superFeatures.add(FEATURE_ADVANCED_AI);
            superFeatures.add(FEATURE_PLATFORM_SUPER);
            return List.copyOf(superFeatures);
        }
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant)) {
            return List.of();
        }
        return List.copyOf(buildFeatureKeys(tenant, false));
    }

    public List<TenantFeatureOptionVO> featureCatalog() {
        List<TenantFeatureOptionVO> features = new ArrayList<>();
        features.add(feature("module.dashboard", "总览大盘", "基础模块", "经营总览、关键指标和待办提醒", true, true));
        features.add(feature("module.order", "订单管理", "基础模块", "销售订单、生产订单和订单流转", true, true));
        features.add(feature("module.inventory", "库存管理", "基础模块", "库存流水、库存预警和出入库", true, true));
        features.add(feature("module.badProduct", "次品管理", "基础模块", "次品登记、处理闭环和损失跟踪", true, true));
        features.add(feature("module.customer", "客户管理", "基础模块", "客户档案、联系人和合作信息", true, true));
        features.add(feature("module.price", "价格管理", "基础模块", "SKU 价格、客户等级价和特价", true, true));
        features.add(feature("module.receipt", "出库单打印", "基础模块", "出库单模板、打印确认和回执", true, true));
        features.add(feature("module.approval", "审批中心", "基础模块", "请假、财务等审批流程", true, true));
        features.add(feature("module.attendance", "考勤管理", "基础模块", "小程序打卡、规则和统计", true, true));
        features.add(feature("module.employee", "员工管理", "基础模块", "员工档案、组织和状态", true, true));
        features.add(feature("module.role", "角色管理", "基础模块", "角色权限和人员授权", true, true));
        features.add(feature("module.label", "标签打印", "基础模块", "标签模板和小程序打印联动", true, true));
        features.add(feature("module.document", "文档管理", "基础模块", "企业目录、文件和 OSS 存储", true, true));
        features.add(feature("module.manual", "使用手册", "基础模块", "网页端用户使用说明", true, true));
        features.add(feature(FEATURE_AI_ADVICE, "AI 建议", "智能能力", "经营、员工、客户和风险建议", false, true));
        features.add(feature(FEATURE_ADVANCED_AI, "高级 AI", "智能能力", "高维建议、闭环进化和高级分析", false, false));
        return features;
    }

    public boolean tryConsumeAiAdviceQuota(String tenantCode) {
        if (isSuperTenant(tenantCode)) {
            return true;
        }
        Tenant tenant = requireTenant(tenantCode);
        if (!isTenantUsable(tenant) || !isFeatureEnabled(tenant, FEATURE_AI_ADVICE)) {
            return false;
        }
        Integer maxAiAdvicePerMonth = tenant.getMaxAiAdvicePerMonth();
        if (maxAiAdvicePerMonth == null) {
            return true;
        }
        if (maxAiAdvicePerMonth <= 0) {
            return false;
        }
        return tryConsumeUsageMeter(tenantCode, METER_AI_ADVICE, currentMonthKey(), maxAiAdvicePerMonth);
    }

    private boolean tryConsumeUsageMeter(String tenantCode, String meterType, String periodKey, int limitCount) {
        try {
            TenantUsageMeter meter = tenantUsageMeterMapper.selectOne(new LambdaQueryWrapper<TenantUsageMeter>()
                    .eq(TenantUsageMeter::getTenantCode, tenantCode)
                    .eq(TenantUsageMeter::getMeterType, meterType)
                    .eq(TenantUsageMeter::getPeriodKey, periodKey)
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now();
            if (meter == null) {
                meter = new TenantUsageMeter();
                meter.setTenantCode(tenantCode);
                meter.setMeterType(meterType);
                meter.setPeriodKey(periodKey);
                meter.setUsedCount(1);
                meter.setLimitCount(limitCount);
                meter.setCreateTime(now);
                meter.setUpdateTime(now);
                tenantUsageMeterMapper.insert(meter);
                return true;
            }

            meter.setLimitCount(limitCount);
            int usedCount = meter.getUsedCount() == null ? 0 : meter.getUsedCount();
            if (usedCount >= limitCount) {
                meter.setUpdateTime(now);
                tenantUsageMeterMapper.updateById(meter);
                return false;
            }
            meter.setUsedCount(usedCount + 1);
            meter.setUpdateTime(now);
            tenantUsageMeterMapper.updateById(meter);
            return true;
        } catch (Exception ex) {
            log.warn("tenant usage meter unavailable, tenantCode={}, meterType={}", tenantCode, meterType, ex);
            return true;
        }
    }

    private TenantFeatureOptionVO feature(String code, String name, String category, String description, boolean baseModule, boolean defaultEnabled) {
        return new TenantFeatureOptionVO(code, name, category, description, baseModule, defaultEnabled);
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
            stringRedisTemplate.opsForHash().delete("companyAttendanceRule", tenantCode);
        } catch (Exception ignored) {
        }
    }

    public boolean isTenantUsable(Tenant tenant) {
        if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted()) || !Integer.valueOf(1).equals(tenant.getStatus())) {
            return false;
        }
        String subscriptionStatus = normalizeStatusOnly(tenant.getSubscriptionStatus());
        if (STATUS_SUSPENDED.equals(subscriptionStatus) || STATUS_EXPIRED.equals(subscriptionStatus)) {
            return false;
        }
        LocalDateTime endTime = tenant.getSubscriptionEndTime();
        return endTime == null || !endTime.isBefore(LocalDateTime.now());
    }

    public boolean isSuperTenant(String tenantCode) {
        return StringUtils.hasText(tenantCode) && SUPER_TENANT_CODE.equalsIgnoreCase(tenantCode.trim());
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
            return STATUS_EXPIRED;
        }
        return PLAN_TRIAL.equals(planCode) ? STATUS_TRIAL : STATUS_ACTIVE;
    }

    private String normalizeStatusOnly(String value) {
        if (!StringUtils.hasText(value)) {
            return STATUS_ACTIVE;
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
                throw new BusinessException("功能开关必须是 JSON 对象");
            }
            buildFeatureKeys(defaultText(planCode, PLAN_TRIAL), trimmed, true, null);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("功能开关必须是合法 JSON");
        }
        return trimmed;
    }

    private String defaultFeatureFlags(String planCode) {
        boolean advancedAi = PLAN_PROFESSIONAL.equals(planCode) || PLAN_PRIVATE.equals(planCode);
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
                + "\"manual\":true"
                + "},"
                + "\"aiAdvice\":true,"
                + "\"advancedAi\":" + advancedAi + ","
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
        String planCode = defaultText(tenant.getPackageCode(), PLAN_TRIAL);
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
                    throw new BusinessException("功能开关必须是 JSON 对象");
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
        LinkedHashSet<String> features = new LinkedHashSet<>(BASE_MODULE_FEATURES);
        features.add(FEATURE_AI_ADVICE);
        if (PLAN_PROFESSIONAL.equals(planCode) || PLAN_PRIVATE.equals(planCode)) {
            features.add(FEATURE_ADVANCED_AI);
        }
        return features;
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

    private String currentMonthKey() {
        return YearMonth.now().toString().replace("-", "");
    }

    private LocalDateTime defaultStartTime(LocalDateTime existed) {
        return existed == null ? LocalDateTime.now() : existed;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record PlanLimit(String name, Integer maxUsers, Integer maxAiAdvicePerMonth, Integer maxStorageMb) {
    }
}
