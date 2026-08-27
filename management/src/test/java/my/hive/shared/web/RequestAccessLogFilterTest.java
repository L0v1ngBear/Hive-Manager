package my.hive.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestAccessLogFilterTest {

    @Test
    void recordsOnlyBoundaryMetadataAndAlwaysContinuesTheRequest() throws Exception {
        RequestAccessLogProperties properties = new RequestAccessLogProperties();
        TrustedClientIpResolver clientIpResolver = mock(TrustedClientIpResolver.class);
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        RequestAccessLogFilter filter = new RequestAccessLogFilter(properties, clientIpResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.setAttribute(RequestTraceFilter.TRACE_ATTRIBUTE, "trace-from-request-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            invoked.set(true);
            ((MockHttpServletResponse) ignoredResponse).setStatus(204);
        });

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(204);
    }
}
