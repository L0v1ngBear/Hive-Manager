package my.hive.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.context.TenantPermissionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Outer request boundary guaranteeing ThreadLocal cleanup for every outcome. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextLifecycleFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            TenantDataSourceContextHolder.clear();
            TenantPermissionContext.clear();
        }
    }
}
