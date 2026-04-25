package my.management.common.config;

import jakarta.annotation.Resource;
import my.management.common.interceptor.AuthTokenInterceptor;
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

    @Resource
    private AuthTokenInterceptor authTokenInterceptor;

    @Value("${app.cors.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/scan-login/session",
                        "/auth/scan-login/status",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
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
