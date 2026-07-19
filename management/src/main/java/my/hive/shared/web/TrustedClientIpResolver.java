package my.hive.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrustedClientIpResolver {

    private final TrustedClientIpProperties properties;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String socketPeer = normalize(request.getRemoteAddr());
        if (!StringUtils.hasText(socketPeer)) {
            return "unknown";
        }
        List<CidrBlock> trusted = trustedBlocks();
        if (!isTrusted(socketPeer, trusted)) {
            return socketPeer;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String[] rawHops = forwardedFor.split(",", -1);
            List<String> hops = new ArrayList<>(rawHops.length);
            for (String rawHop : rawHops) {
                String hop = normalize(rawHop);
                if (!StringUtils.hasText(hop)) {
                    return socketPeer;
                }
                hops.add(hop);
            }
            for (int index = hops.size() - 1; index >= 0; index -= 1) {
                String hop = hops.get(index);
                if (!isTrusted(hop, trusted)) {
                    return hop;
                }
            }
            return hops.isEmpty() ? socketPeer : hops.get(0);
        }

        String realIp = normalize(request.getHeader("X-Real-IP"));
        return StringUtils.hasText(realIp) ? realIp : socketPeer;
    }

    private List<CidrBlock> trustedBlocks() {
        List<CidrBlock> result = new ArrayList<>();
        if (properties.getTrustedProxies() == null) {
            return result;
        }
        for (String value : properties.getTrustedProxies()) {
            CidrBlock block = CidrBlock.parse(value);
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    private boolean isTrusted(String address, List<CidrBlock> blocks) {
        byte[] bytes = addressBytes(address);
        if (bytes == null) {
            return false;
        }
        return blocks.stream().anyMatch(block -> block.contains(bytes));
    }

    private String normalize(String rawAddress) {
        if (!StringUtils.hasText(rawAddress)) {
            return null;
        }
        String value = rawAddress.trim();
        if (value.startsWith("[") && value.endsWith("]") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        byte[] bytes = addressBytes(value);
        if (bytes == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private byte[] addressBytes(String value) {
        if (!StringUtils.hasText(value) || !value.matches("^[0-9A-Fa-f:.]+$")) {
            return null;
        }
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private record CidrBlock(byte[] network, int prefixLength) {
        static CidrBlock parse(String raw) {
            if (!StringUtils.hasText(raw)) {
                return null;
            }
            String[] parts = raw.trim().split("/", -1);
            if (parts.length != 2 || !parts[1].matches("^\\d{1,3}$")) {
                return null;
            }
            try {
                byte[] network = InetAddress.getByName(parts[0]).getAddress();
                int prefix = Integer.parseInt(parts[1]);
                if (prefix < 0 || prefix > network.length * 8) {
                    return null;
                }
                return new CidrBlock(network, prefix);
            } catch (UnknownHostException exception) {
                return null;
            }
        }

        boolean contains(byte[] address) {
            if (address == null || address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int partialBits = prefixLength % 8;
            for (int index = 0; index < fullBytes; index += 1) {
                if (address[index] != network[index]) {
                    return false;
                }
            }
            if (partialBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - partialBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
