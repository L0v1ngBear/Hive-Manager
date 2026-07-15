package my.hive.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BuildIdentityFilter extends OncePerRequestFilter {

    public static final String BUILD_HEADER = "X-Hive-Build";
    public static final String INSTANCE_HEADER = "X-Hive-Instance";

    private final String buildIdentity;
    private final String instanceIdentity = UUID.randomUUID().toString();

    public BuildIdentityFilter(BuildProperties buildProperties) {
        Instant buildTime = buildProperties.getTime();
        this.buildIdentity = buildProperties.getName()
                + ":" + buildProperties.getVersion()
                + ":" + (buildTime == null ? "unknown" : buildTime.toString());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(BUILD_HEADER, buildIdentity);
        response.setHeader(INSTANCE_HEADER, instanceIdentity);
        filterChain.doFilter(request, response);
    }
}
