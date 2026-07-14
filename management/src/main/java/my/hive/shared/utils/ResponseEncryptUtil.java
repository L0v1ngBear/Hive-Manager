package my.hive.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import my.hive.shared.model.EncryptPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 响应加密工具。
 *
 * <p>服务端只保存一个主密钥，登录成功后根据 token 派生会话级响应密钥返回给前端。
 * 这样生产主密钥不会进入浏览器或小程序构建包，后续业务响应仍然可以进行 AES 加密和 HMAC 验签。</p>
 */
@Component
public class ResponseEncryptUtil {

    public static final String RESPONSE_ALGORITHM = "AES-256-CBC+HMAC-SHA256";
    private static final String KEY_DERIVE_ALGORITHM = "HmacSHA512";
    private static final String KEY_DERIVE_PREFIX = "hive-response:";
    private static final int KEY_MATERIAL_LENGTH = 64;
    private static final int IV_LENGTH = 16;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${security.response.key}")
    private String responseKey;

    private final SecureRandom secureRandom = new SecureRandom();
    private Base64.Encoder base64Encoder;
    private byte[] masterKeyMaterial;

    @PostConstruct
    public void init() {
        base64Encoder = Base64.getEncoder();
        masterKeyMaterial = Base64.getDecoder().decode(responseKey);
        if (masterKeyMaterial.length < KEY_MATERIAL_LENGTH) {
            throw new IllegalStateException("security.response.key must be at least 64 bytes after Base64 decode");
        }
    }

    public String buildResponseKey(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token is required to build response key");
        }
        return base64Encoder.encodeToString(deriveKeyMaterial(token));
    }

    public EncryptPayload encrypt(String token, Object data) {
        try {
            byte[] keyMaterial = resolveKeyMaterial(token);
            byte[] cipherKey = Arrays.copyOfRange(keyMaterial, 0, 32);
            byte[] macKey = Arrays.copyOfRange(keyMaterial, 32, 64);
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cipherKey, "AES"), new IvParameterSpec(iv));
            byte[] plainBytes = objectMapper.writeValueAsBytes(data);
            byte[] cipherBytes = cipher.doFinal(plainBytes);
            String ivBase64 = base64Encoder.encodeToString(iv);
            String cipherBase64 = base64Encoder.encodeToString(cipherBytes);

            EncryptPayload payload = new EncryptPayload();
            payload.setIv(ivBase64);
            payload.setCiphertext(cipherBase64);
            payload.setMac(sign(macKey, ivBase64 + "." + cipherBase64));
            return payload;
        } catch (Exception ex) {
            throw new IllegalStateException("response encrypt failed", ex);
        }
    }

    private byte[] resolveKeyMaterial(String token) {
        if (StringUtils.hasText(token)) {
            return deriveKeyMaterial(token);
        }
        return Arrays.copyOf(masterKeyMaterial, KEY_MATERIAL_LENGTH);
    }

    private byte[] deriveKeyMaterial(String token) {
        try {
            Mac mac = Mac.getInstance(KEY_DERIVE_ALGORITHM);
            mac.init(new SecretKeySpec(masterKeyMaterial, KEY_DERIVE_ALGORITHM));
            return mac.doFinal((KEY_DERIVE_PREFIX + token).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("response key derive failed", ex);
        }
    }

    private String sign(byte[] macKey, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(macKey, "HmacSHA256"));
            return base64Encoder.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("response sign failed", ex);
        }
    }
}
