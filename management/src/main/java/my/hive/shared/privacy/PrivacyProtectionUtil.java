package my.hive.shared.privacy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 隐私数据保护工具。
 *
 * 手机号等个人信息在业务里通常还要支持登录、查重和搜索，因此这里采用“不可逆哈希 + 脱敏展示”的组合：
 * - hashPhone：只用于数据库精确匹配，不允许还原手机号。
 * - maskPhone：只用于前端展示和导出，避免把完整手机号暴露给非必要场景。
 */
@Component
public class PrivacyProtectionUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PHONE_HASH_SCOPE = "phone:";

    @Value("${security.privacy.hash-secret:${auth.token.secret:hive-dev-privacy-secret}}")
    private String hashSecret;

    /**
     * 统一手机号格式，避免同一个号码因为空格、横线或 +86 前缀导致哈希不同。
     */
    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("86") && digits.length() == 13) {
            digits = digits.substring(2);
        }
        return digits.isBlank() ? null : digits;
    }

    /**
     * 生成不可逆手机号哈希。
     *
     * 说明：使用带密钥的 HMAC，而不是普通 SHA-256，避免攻击者用常见手机号字典直接撞库。
     */
    public String hashPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            return null;
        }
        return hmacSha256(PHONE_HASH_SCOPE + normalizedPhone);
    }

    /**
     * 手机号脱敏展示。常见 11 位手机号展示为 138****1234。
     */
    public String maskPhone(String phone) {
        if (isMasked(phone)) {
            return clean(phone);
        }
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            return clean(phone);
        }
        if (normalizedPhone.length() >= 11) {
            return normalizedPhone.substring(0, 3) + "****" + normalizedPhone.substring(normalizedPhone.length() - 4);
        }
        if (normalizedPhone.length() > 7) {
            return normalizedPhone.substring(0, 3) + "****" + normalizedPhone.substring(normalizedPhone.length() - 2);
        }
        if (normalizedPhone.length() > 3) {
            return normalizedPhone.substring(0, 2) + "****";
        }
        return "****";
    }

    /**
     * 返回优先级：已落库脱敏值 > 根据原值临时脱敏 > 空。
     */
    public String displayPhone(String phone, String phoneMask) {
        if (phoneMask != null && !phoneMask.isBlank()) {
            return phoneMask.trim();
        }
        return maskPhone(phone);
    }

    /**
     * 粗略判断关键字是否像手机号，用于决定是否追加 phone_hash 精确匹配条件。
     */
    public boolean mayBePhoneKeyword(String keyword) {
        String normalizedPhone = normalizePhone(keyword);
        return normalizedPhone != null && normalizedPhone.length() >= 7;
    }

    /**
     * 前端编辑表单可能回传 138****1234 这样的脱敏值；这种值只能展示，不能重新哈希落库。
     */
    public boolean isMasked(String value) {
        return value != null && value.contains("*");
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("手机号隐私哈希生成失败", exception);
        }
    }

    private String resolveSecret() {
        if (hashSecret == null || hashSecret.isBlank()) {
            return "hive-dev-privacy-secret";
        }
        return hashSecret;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
