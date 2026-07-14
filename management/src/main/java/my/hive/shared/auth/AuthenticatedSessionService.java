package my.hive.shared.auth;

import org.springframework.stereotype.Service;

/** Resolves authenticated sessions through the single token implementation. */
@Service
public class AuthenticatedSessionService {

    private final TokenService tokenService;

    public AuthenticatedSessionService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public AuthUserInfo authenticate(String token) {
        return tokenService.parse(token);
    }
}
