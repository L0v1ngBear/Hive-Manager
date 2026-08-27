package my.hive.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Records one structured completion log for each API request without payloads. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
@RequiredArgsConstructor
public class RequestAccessLogFilter extends OncePerRequestFilter {

    private final RequestAccessLogProperties properties;
    private final TrustedClientIpResolver trustedClientIpResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPathPrefixes() != null
                && properties.getExcludedPathPrefixes().stream()
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .anyMatch(prefix -> path.startsWith(prefix.trim()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            String traceId = String.valueOf(request.getAttribute(RequestTraceFilter.TRACE_ATTRIBUTE));
            boolean slow = durationMs >= Math.max(0L, properties.getSlowThresholdMs());
            String message = "HIVE_ACCESS_LOG traceId={}, method={}, path={}, status={}, durationMs={}, slow={}, clientIp={}";
            if (response.getStatus() >= 500) {
                log.error(message, traceId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        durationMs, slow, trustedClientIpResolver.resolve(request));
            } else if (response.getStatus() >= 400 || slow) {
                log.warn(message, traceId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        durationMs, slow, trustedClientIpResolver.resolve(request));
            } else {
                log.info(message, traceId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        durationMs, slow, trustedClientIpResolver.resolve(request));
            }
        }
    }
}
