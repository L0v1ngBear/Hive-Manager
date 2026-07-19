package my.hive.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedClientIpResolverTest {

    private final TrustedClientIpProperties properties = new TrustedClientIpProperties();
    private final TrustedClientIpResolver resolver = new TrustedClientIpResolver(properties);

    @Test
    void ignoresForwardedHeadersFromUntrustedSocketPeer() {
        MockHttpServletRequest request = request("198.51.100.8");
        request.addHeader("X-Forwarded-For", "203.0.113.4");
        request.addHeader("X-Real-IP", "203.0.113.5");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.8");
    }

    @Test
    void resolvesFirstUntrustedHopFromRightWhenLocalProxyIsTrusted() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.4, 10.0.0.7, 127.0.0.1");
        properties.setTrustedProxies(List.of("127.0.0.0/8", "10.0.0.0/8", "::1/128"));

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.4");
    }

    @Test
    void fallsBackToTrustedSocketPeerWhenForwardedChainIsMalformed() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "secret-ticket, 127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void normalizesIpv4MappedIpv6AndUsesRealIpOnlyBehindTrustedProxy() {
        MockHttpServletRequest request = request("::1");
        request.addHeader("X-Real-IP", "::ffff:192.0.2.9");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.9");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
