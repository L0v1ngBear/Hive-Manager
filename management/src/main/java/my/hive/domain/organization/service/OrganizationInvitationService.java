package my.hive.domain.organization.service;

import lombok.RequiredArgsConstructor;
import my.hive.domain.auth.model.AuthReason;
import my.hive.domain.organization.model.OrganizationInvitationPayload;
import my.hive.domain.organization.model.vo.OrganizationJoinCodeVO;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationInvitationService {

    private static final String KEY_PART = "organization-join-code";
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int MAX_GENERATION_ATTEMPTS = 8;
    private static final int MAX_CONFIGURED_USES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> ISSUE_SCRIPT = issueScript();
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> CONSUME_SCRIPT = consumeScript();

    private final StringRedisTemplate stringRedisTemplate;
    private final HiveRedisKeyBuilder redisKeyBuilder;

    @Value("${auth.invitation.ttl-seconds:900}")
    private long ttlSeconds = 900L;

    @Value("${auth.invitation.max-uses:1}")
    private int maxUses = 1;

    public OrganizationJoinCodeVO issue(String tenantCode, Long issuerUserId) {
        if (!StringUtils.hasText(tenantCode) || issuerUserId == null || issuerUserId <= 0) {
            throw new BusinessException(401, "当前登录组织异常，请重新登录");
        }
        long safeTtl = Math.max(60L, Math.min(ttlSeconds, 3600L));
        int safeUses = Math.max(1, Math.min(maxUses, MAX_CONFIGURED_USES));
        long expiresAtMillis = System.currentTimeMillis() + safeTtl * 1000L;

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt += 1) {
            String code = generateCode();
            Long created = stringRedisTemplate.execute(
                    ISSUE_SCRIPT,
                    List.of(key(code)),
                    tenantCode.trim(),
                    Long.toString(issuerUserId),
                    Long.toString(expiresAtMillis),
                    Integer.toString(safeUses),
                    Long.toString(safeTtl)
            );
            if (Long.valueOf(1L).equals(created)) {
                OrganizationJoinCodeVO result = new OrganizationJoinCodeVO();
                result.setOrganizationCode(code);
                result.setExpiresInSeconds(safeTtl);
                result.setExpireAt(expiresAtMillis / 1000L);
                return result;
            }
        }
        throw new BusinessException(500, "组织邀请码生成失败，请稍后重试");
    }

    public OrganizationInvitationPayload consume(String rawCode) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            throw invalidInvitation();
        }
        @SuppressWarnings("unchecked")
        List<Object> result = stringRedisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(key(code)),
                Long.toString(System.currentTimeMillis())
        );
        if (result == null || result.size() < 4 || !StringUtils.hasText(asText(result.get(0)))) {
            throw invalidInvitation();
        }
        try {
            OrganizationInvitationPayload payload = new OrganizationInvitationPayload();
            payload.setTenantCode(asText(result.get(0)).trim());
            String issuer = asText(result.get(1));
            payload.setIssuerUserId(StringUtils.hasText(issuer) ? Long.valueOf(issuer) : null);
            payload.setExpiresAt(Long.valueOf(asText(result.get(2))));
            payload.setRemainingUses(Integer.valueOf(asText(result.get(3))));
            return payload;
        } catch (RuntimeException exception) {
            throw invalidInvitation();
        }
    }

    private String key(String code) {
        return redisKeyBuilder.cache("auth", KEY_PART, code);
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.matches("^[A-Z0-9]{4,32}$") ? normalized : null;
    }

    private String generateCode() {
        StringBuilder result = new StringBuilder(8);
        for (int index = 0; index < 8; index += 1) {
            result.append(CODE_CHARS[SECURE_RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return result.toString();
    }

    private String asText(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value == null ? "" : String.valueOf(value);
    }

    private BusinessException invalidInvitation() {
        return new BusinessException(
                400,
                AuthReason.INVITATION_INVALID_OR_EXPIRED,
                "组织邀请码无效或已过期，请联系企业负责人重新获取"
        );
    }

    private static DefaultRedisScript<Long> issueScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                if redis.call('EXISTS', KEYS[1]) == 1 then
                    return 0
                end
                redis.call('HSET', KEYS[1],
                    'tenantCode', ARGV[1],
                    'issuerUserId', ARGV[2],
                    'expiresAt', ARGV[3],
                    'remainingUses', ARGV[4])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[5]))
                return 1
                """);
        return script;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static DefaultRedisScript<List> consumeScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local keyType = redis.call('TYPE', KEYS[1])
                if type(keyType) == 'table' then keyType = keyType.ok end
                if keyType == 'none' then return {} end
                if keyType == 'string' then
                    local tenantCode = redis.call('GET', KEYS[1])
                    if not tenantCode or tenantCode == '' then return {} end
                    redis.call('DEL', KEYS[1])
                    return {tenantCode, '', '0', '0'}
                end
                if keyType ~= 'hash' then return {} end

                local tenantCode = redis.call('HGET', KEYS[1], 'tenantCode')
                local issuerUserId = redis.call('HGET', KEYS[1], 'issuerUserId') or ''
                local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
                local remainingUses = tonumber(redis.call('HGET', KEYS[1], 'remainingUses'))
                local now = tonumber(ARGV[1])
                if not tenantCode or tenantCode == '' or not expiresAt or not remainingUses
                        or expiresAt <= now or remainingUses <= 0 then
                    redis.call('DEL', KEYS[1])
                    return {}
                end

                local afterUse = remainingUses - 1
                if afterUse <= 0 then
                    redis.call('DEL', KEYS[1])
                else
                    redis.call('HINCRBY', KEYS[1], 'remainingUses', -1)
                end
                return {tenantCode, issuerUserId, tostring(expiresAt), tostring(afterUse)}
                """);
        return script;
    }
}
