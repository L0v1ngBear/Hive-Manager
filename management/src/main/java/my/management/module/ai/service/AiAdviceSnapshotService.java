package my.management.module.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.event.SystemEvent;
import my.hive.common.event.SystemEventPublisher;
import my.hive.common.redis.HiveRedisKeyBuilder;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.module.ai.mapper.AiAdviceSnapshotMapper;
import my.management.module.ai.model.entity.AiAdviceSnapshot;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.service.TenantLicenseService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AiAdviceSnapshotService {

    private static final Duration SNAPSHOT_LOCK_TTL = Duration.ofMinutes(45);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Resource
    private AiAdviceSnapshotMapper aiAdviceSnapshotMapper;

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HiveRedisKeyBuilder redisKeyBuilder;

    @Resource
    private SystemEventPublisher systemEventPublisher;

    @Resource
    private TenantLicenseService tenantLicenseService;

    public List<DashboardAiAdviceVO> readSnapshotAdvices(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }
        try {
            AiAdviceSnapshot snapshot = aiAdviceSnapshotMapper.selectByTenantCode(tenantCode.trim());
            if (snapshot == null || snapshot.getSnapshotJson() == null || snapshot.getSnapshotJson().isBlank()) {
                return List.of();
            }
            List<DashboardAiAdviceVO> advices = objectMapper.readValue(
                    snapshot.getSnapshotJson(),
                    new TypeReference<List<DashboardAiAdviceVO>>() {
                    }
            );
            if (advices == null || advices.isEmpty()) {
                return List.of();
            }
            return advices.stream().filter(Objects::nonNull).toList();
        } catch (Exception ex) {
            log.warn("read ai advice snapshot failed, tenantCode={}", tenantCode, ex);
            return List.of();
        }
    }

    public Map<String, Object> refreshAllTenants() {
        String lockKey = redisKeyBuilder.lock("management", "ai-advice", "snapshot-refresh");
        boolean locked = tryAcquireLock(lockKey);
        Map<String, Object> result = new LinkedHashMap<>();
        if (!locked) {
            result.put("skipped", true);
            result.put("reason", "another ai advice snapshot refresh is running");
            return result;
        }

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        try {
            List<Tenant> tenants = tenantMapper.selectList(null);
            if (tenants == null || tenants.isEmpty()) {
                result.put("successCount", 0);
                result.put("failedCount", 0);
                result.put("skippedCount", 0);
                return result;
            }

            for (Tenant tenant : tenants) {
                if (!isActiveTenant(tenant) || !tenantLicenseService.tryConsumeAiAdviceQuota(tenant.getTenantCode())) {
                    skippedCount++;
                    continue;
                }
                String tenantCode = tenant.getTenantCode();
                try {
                    refreshTenantInternal(tenantCode);
                    successCount++;
                } catch (Exception ex) {
                    failedCount++;
                    log.error("refresh ai advice snapshot failed, tenantCode={}", tenantCode, ex);
                    markFailureSafely(tenantCode, ex);
                }
            }

            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            result.put("skippedCount", skippedCount);
            publishRefreshEvent(successCount, failedCount, skippedCount);
            return result;
        } finally {
            releaseLock(lockKey);
        }
    }

    public List<DashboardAiAdviceVO> refreshTenant(String tenantCode) throws Exception {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode cannot be blank");
        }
        if (!tenantLicenseService.tryConsumeAiAdviceQuota(tenantCode)) {
            throw new IllegalStateException("AI advice quota exhausted or feature disabled for tenant: " + tenantCode);
        }
        return refreshTenantInternal(tenantCode);
    }

    private List<DashboardAiAdviceVO> refreshTenantInternal(String tenantCode) throws Exception {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode cannot be blank");
        }
        String safeTenantCode = tenantCode.trim();
        List<DashboardAiAdviceVO> advices = aiAnalysisService.buildAllDashboardAdvices(safeTenantCode, fullVisibility());
        List<DashboardAiAdviceVO> safeAdvices = advices == null
                ? List.of()
                : advices.stream().filter(Objects::nonNull).toList();

        AiAdviceSnapshot snapshot = new AiAdviceSnapshot();
        snapshot.setTenantCode(safeTenantCode);
        snapshot.setSnapshotJson(objectMapper.writeValueAsString(safeAdvices));
        snapshot.setAdviceCount(safeAdvices.size());
        aiAdviceSnapshotMapper.upsertSuccess(snapshot);
        return safeAdvices;
    }

    private boolean isActiveTenant(Tenant tenant) {
        return tenant != null
                && tenant.getTenantCode() != null
                && !tenant.getTenantCode().isBlank()
                && !DeleteFlagEnum.isDeleted(tenant.getDeleted())
                && (tenant.getStatus() == null || CommonStatusEnum.isEnabled(tenant.getStatus()));
    }

    private DashboardOverviewVO.Visibility fullVisibility() {
        DashboardOverviewVO.Visibility visibility = new DashboardOverviewVO.Visibility();
        visibility.setOrderVisible(true);
        visibility.setInventoryVisible(true);
        visibility.setApprovalVisible(true);
        visibility.setReceiptVisible(true);
        visibility.setTrendVisible(true);
        visibility.setAttendanceVisible(true);
        visibility.setAiAdviceVisible(true);
        return visibility;
    }

    private boolean tryAcquireLock(String lockKey) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, String.valueOf(System.currentTimeMillis()), SNAPSHOT_LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            log.warn("redis lock unavailable for ai advice snapshot refresh, continue without distributed lock", ex);
            return true;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
        } catch (Exception ignored) {
        }
    }

    private void markFailureSafely(String tenantCode, Exception ex) {
        try {
            aiAdviceSnapshotMapper.markFailure(tenantCode, truncate(ex.getMessage(), MAX_ERROR_MESSAGE_LENGTH));
        } catch (Exception ignored) {
        }
    }

    private void publishRefreshEvent(int successCount, int failedCount, int skippedCount) {
        Map<String, Object> detail = Map.of(
                "successCount", successCount,
                "failedCount", failedCount,
                "skippedCount", skippedCount
        );
        systemEventPublisher.publish(SystemEvent.builder()
                .eventType("AI_ADVICE_SNAPSHOT_REFRESH")
                .level(failedCount > 0 ? "WARN" : "INFO")
                .module("ai")
                .title(failedCount > 0 ? "AI advice snapshot refresh had failures" : "AI advice snapshot refreshed")
                .content("success=" + successCount + ", failed=" + failedCount + ", skipped=" + skippedCount)
                .bizType("xxl-job")
                .bizNo("aiAdviceSnapshotRefreshJob")
                .detail(detail)
                .build());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "unknown error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
