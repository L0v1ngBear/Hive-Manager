package my.hive.domain.organization.service;

import my.hive.domain.auth.model.AuthReason;
import my.hive.domain.organization.model.OrganizationInvitationPayload;
import my.hive.domain.organization.model.vo.OrganizationJoinCodeVO;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationInvitationServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final HiveRedisKeyBuilder keys = mock(HiveRedisKeyBuilder.class);
    private final OrganizationInvitationService service = new OrganizationInvitationService(redis, keys);

    @BeforeEach
    void setUp() {
        when(keys.cache(eq("auth"), eq("organization-join-code"), anyString()))
                .thenAnswer(invocation -> "auth:organization-join-code:" + invocation.getArgument(2));
    }

    @Test
    void issuesShortLivedOneUseStructuredPayloadAtomically() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class),
                anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1L);

        long before = System.currentTimeMillis();
        OrganizationJoinCodeVO issued = service.issue("tenant-a", 7L);

        assertThat(issued.getOrganizationCode()).matches("^[A-Z2-9]{8}$");
        assertThat(issued.getExpiresInSeconds()).isEqualTo(900L);
        assertThat(issued.getExpireAt()).isBetween(before / 1000 + 899, before / 1000 + 901);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DefaultRedisScript> script = ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis).execute(script.capture(),
                eq(List.of("auth:organization-join-code:" + issued.getOrganizationCode())),
                eq("tenant-a"), eq("7"), anyString(), eq("1"), eq("900"));
        assertThat(script.getValue().getScriptAsString())
                .contains("EXISTS", "HSET", "tenantCode", "issuerUserId", "expiresAt", "remainingUses", "EXPIRE")
                .doesNotContain("SET', KEYS[1], ARGV[1]");
    }

    @Test
    void consumesStructuredPayloadWithOneAtomicValidateAndDecrementScript() {
        long expiresAt = System.currentTimeMillis() + 60_000;
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:organization-join-code:JOIN1234")), anyString()))
                .thenReturn(List.of("tenant-a", "7", Long.toString(expiresAt), "0"));

        OrganizationInvitationPayload payload = service.consume("join1234");

        assertThat(payload.getTenantCode()).isEqualTo("tenant-a");
        assertThat(payload.getIssuerUserId()).isEqualTo(7L);
        assertThat(payload.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(payload.getRemainingUses()).isZero();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DefaultRedisScript> script = ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis).execute(script.capture(),
                eq(List.of("auth:organization-join-code:JOIN1234")), anyString());
        assertThat(script.getValue().getScriptAsString())
                .contains("TYPE", "HGET", "expiresAt", "remainingUses", "HINCRBY", "DEL");
    }

    @Test
    void consumesShortLivedLegacyStringOnlyOnce() {
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:organization-join-code:LEGACY12")), anyString()))
                .thenReturn(List.of("tenant-a", "", "0", "0"), List.of());

        assertThat(service.consume("LEGACY12").getTenantCode()).isEqualTo("tenant-a");
        assertInvalid(() -> service.consume("LEGACY12"));
    }

    @Test
    void allowsExactlyOneConcurrentWinnerForDefaultOneUseCode() throws Exception {
        AtomicInteger redisWinners = new AtomicInteger();
        long expiresAt = System.currentTimeMillis() + 60_000;
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("auth:organization-join-code:ONCE1234")), anyString()))
                .thenAnswer(invocation -> redisWinners.getAndIncrement() == 0
                        ? List.of("tenant-a", "7", Long.toString(expiresAt), "0")
                        : List.of());

        int callers = 24;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(callers);
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int index = 0; index < callers; index += 1) {
            attempts.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    service.consume("ONCE1234");
                    return true;
                } catch (BusinessException exception) {
                    return false;
                }
            });
        }
        var futures = attempts.stream().map(executor::submit).toList();
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        long successes = 0;
        for (var future : futures) {
            if (future.get(5, TimeUnit.SECONDS)) {
                successes += 1;
            }
        }
        executor.shutdownNow();

        assertThat(successes).isEqualTo(1L);
    }

    @Test
    void invalidExpiredOrUsedCodeReturnsStableChineseReason() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), anyString())).thenReturn(List.of());

        assertInvalid(() -> service.consume("MISSING1"));
        assertInvalid(() -> service.consume("invalid code"));
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(400, AuthReason.INVITATION_INVALID_OR_EXPIRED, "组织邀请码无效或已过期，请联系企业负责人重新获取");
    }
}
