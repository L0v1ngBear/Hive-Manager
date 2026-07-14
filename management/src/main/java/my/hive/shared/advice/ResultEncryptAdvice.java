package my.hive.shared.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.dto.Result;
import my.hive.shared.utils.ResponseEncryptUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应加密增强。
 *
 * <p>登录、扫码会话等未携带 token 的公开接口保持明文响应，依赖 HTTPS 保护传输。
 * 携带 Bearer token 的业务接口才会加密，避免前端为了登录前解密而内置生产主密钥。</p>
 */
@ControllerAdvice
public class ResultEncryptAdvice implements ResponseBodyAdvice<Object> {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ResponseEncryptUtil responseEncryptUtil;

    public ResultEncryptAdvice(ResponseEncryptUtil responseEncryptUtil) {
        this.responseEncryptUtil = responseEncryptUtil;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof Result result)) {
            return body;
        }
        if (Boolean.TRUE.equals(result.getEncrypted()) || result.getData() == null) {
            return result;
        }

        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        String token = extractBearerToken(servletRequest);
        if (!shouldEncrypt(servletRequest, selectedContentType, servletResponse, token)) {
            return result;
        }

        result.setData(responseEncryptUtil.encrypt(token, result.getData()));
        result.setEncrypted(true);
        result.setAlg(ResponseEncryptUtil.RESPONSE_ALGORITHM);
        return result;
    }

    private boolean shouldEncrypt(HttpServletRequest request, MediaType mediaType, HttpServletResponse response, String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String path = request.getRequestURI();
        if (isPublicAuthResponse(path)) {
            return false;
        }
        if (mediaType == null || !MediaType.APPLICATION_JSON.includes(mediaType)) {
            return false;
        }
        if (response.getContentType() != null && response.getContentType().contains("application/octet-stream")) {
            return false;
        }
        return path == null || !path.contains("/swagger");
    }

    private boolean isPublicAuthResponse(String path) {
        if (path == null) {
            return false;
        }
        return path.endsWith("/auth/admin/login")
                || path.endsWith("/auth/mini/login")
                || path.endsWith("/auth/mini/wechat-login")
                || path.contains("/auth/admin/scan-login/");
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
