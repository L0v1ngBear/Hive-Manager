package my.hive.shared.tenant;

import jakarta.servlet.FilterChain;
import my.hive.shared.context.TenantPermissionContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextLifecycleFilterTest {
    @Test
    void clearsContextWhenAuthenticatedInitializerRejectsRequest() throws Exception {
        TenantContextLifecycleFilter filter = new TenantContextLifecycleFilter();
        FilterChain chain = (request, response) -> TenantPermissionContext.init("tenant-a", 1L, Set.of("order:list"));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        assertThat(TenantPermissionContext.getTenantCode()).isNull();
        assertThat(TenantDataSourceContextHolder.get()).isNull();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(TenantPermissionContext.getTenantCode()).isNull();
            assertThat(TenantDataSourceContextHolder.get()).isNull();
        });
    }

    @Test
    void clearsContextWhenAuthenticatedInitializerThrowsAndNextSameThreadStartsClean() {
        TenantContextLifecycleFilter filter = new TenantContextLifecycleFilter();
        FilterChain chain = (request, response) -> {
            TenantPermissionContext.init("tenant-a", 1L, Set.of("order:list"));
            TenantDataSourceContextHolder.set("tenant-a-db");
            throw new IllegalStateException("lookup failed");
        };
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(TenantPermissionContext.getTenantCode()).isNull();
        assertThat(TenantDataSourceContextHolder.get()).isNull();
        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
                assertThat(TenantPermissionContext.getTenantCode()).isNull();
                assertThat(TenantDataSourceContextHolder.get()).isNull();
            });
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
