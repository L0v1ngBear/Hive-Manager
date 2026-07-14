package my.hive.shared.auth;

import my.hive.shared.utils.TokenUtil;
import org.springframework.stereotype.Service;

/** Canonical token contract for every Hive client. */
@Service
public class TokenService {

    public String create(Long userId, String tenantCode, Long authVersion) {
        return TokenUtil.createToken(userId, tenantCode, authVersion);
    }

    public AuthUserInfo parse(String token) {
        return TokenUtil.parseToken(token);
    }
}
