package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LogisticsTrackingGateway {

    private final String providerCode;
    private final Map<String, LogisticsTrackingProvider> providers;

    public LogisticsTrackingGateway(@Value("${logistics.provider:apispace}") String providerCode,
                                    List<LogisticsTrackingProvider> providers) {
        this.providerCode = normalize(providerCode);
        Map<String, LogisticsTrackingProvider> providerMap = new LinkedHashMap<>();
        for (LogisticsTrackingProvider provider : providers) {
            String code = normalize(provider.providerCode());
            if (providerMap.putIfAbsent(code, provider) != null) {
                throw new IllegalArgumentException("Duplicate logistics tracking provider: " + code);
            }
        }
        this.providers = Map.copyOf(providerMap);
    }

    public OrderLogisticsTrackingVO query(LogisticsTrackingQuery query) {
        LogisticsTrackingProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new BusinessException(503, "物流查询供应商未配置或不受支持");
        }
        return provider.query(query);
    }

    public String providerCode() {
        return providerCode;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
