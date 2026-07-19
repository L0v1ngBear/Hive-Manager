package my.hive.domain.auth.service;

import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicAuthRateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final HiveRedisKeyBuilder keys = mock(HiveRedisKeyBuilder.class);
    private final PublicAuthRateLimiter limiter = new PublicAuthRateLimiter(redis, keys);

    @BeforeEach
    void setUp() {
        when(keys.counter(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> String.join(":",
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                        invocation.getArgument(3), invocation.getArgument(4)));
    }

    @Test
    void permitsAtThresholdAndRejectsNextHit() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), anyString()))
                .thenReturn(3L, 4L);

        assertThatCode(() -> limiter.check("sms-send", "phone", "phone-hash", 3, Duration.ofMinutes(5)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.check("sms-send", "phone", "phone-hash", 3, Duration.ofMinutes(5)))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(429, "请求过于频繁，请稍后再试");
    }

    @Test
    void usesOneAtomicRedisScriptWithWindowTtl() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), anyString())).thenReturn(1L);

        limiter.check("wechat-login", "ip", "203.0.113.4", 30, Duration.ofMinutes(5));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DefaultRedisScript> script = ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis).execute(script.capture(), any(List.class), eq("300"));
        assertThat(script.getValue().getScriptAsString())
                .contains("redis.call('INCR', KEYS[1])")
                .contains("if current == 1")
                .contains("redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))");
    }

    @Test
    void isolatesDimensionsAndFingerprintsRawSubjectsInRedisKeys() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), anyString())).thenReturn(1L);

        limiter.check("tenant-selection", "ip", "203.0.113.4", 20, Duration.ofMinutes(5));
        limiter.check("tenant-selection", "ticket", "selection-ticket-secret", 5, Duration.ofMinutes(5));

        ArgumentCaptor<List<String>> redisKeys = ArgumentCaptor.forClass(List.class);
        verify(redis, org.mockito.Mockito.times(2))
                .execute(any(DefaultRedisScript.class), redisKeys.capture(), eq("300"));
        assertThat(redisKeys.getAllValues().get(0)).isNotEqualTo(redisKeys.getAllValues().get(1));
        assertThat(redisKeys.getAllValues())
                .allSatisfy(value -> assertThat(value.get(0))
                        .doesNotContain("203.0.113.4", "selection-ticket-secret"));
    }
}
