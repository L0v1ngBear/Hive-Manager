package my.hive.shared.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    void reusesSafeClientTraceAndAlwaysClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(RequestTraceFilter.TRACE_HEADER, "safe-client-trace_1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(RequestTraceFilter.MDC_TRACE_ID)).isEqualTo("safe-client-trace_1234"));

        assertThat(response.getHeader(RequestTraceFilter.TRACE_HEADER)).isEqualTo("safe-client-trace_1234");
        assertThat(request.getAttribute(RequestTraceFilter.TRACE_ATTRIBUTE)).isEqualTo("safe-client-trace_1234");
        assertThat(MDC.get(RequestTraceFilter.MDC_TRACE_ID)).isNull();
    }

    @Test
    void replacesUnsafeClientTraceWithoutReflectingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(RequestTraceFilter.TRACE_HEADER, "unsafe trace\r\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        String traceId = response.getHeader(RequestTraceFilter.TRACE_HEADER);
        assertThat(traceId).matches("^[A-Za-z0-9_-]{8,64}$");
        assertThat(traceId).doesNotContain("unsafe");
    }
}
