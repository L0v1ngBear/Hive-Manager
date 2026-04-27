package my.management.common.config;

import jakarta.annotation.Resource;
import my.management.common.interceptor.AuthTokenInterceptor;
import my.management.common.interceptor.PlatformScopeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * WebMvcConfig 属于管理端后端通用能力层，定义框架配置，用于组织基础设施行为。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/scan-login/session",
            "/auth/scan-login/status",
            "/web/auth/login",
            "/web/auth/scan-login/session",
            "/web/auth/scan-login/status",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    };

    @Resource
    private AuthTokenInterceptor authTokenInterceptor;

    @Resource
    private PlatformScopeInterceptor platformScopeInterceptor;

    @Value("${app.cors.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATHS);
        registry.addInterceptor(platformScopeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATHS);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(resolveAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private String[] resolveAllowedOrigins() {
        return allowedOriginPatterns.split("\\s*,\\s*");
    }
}
