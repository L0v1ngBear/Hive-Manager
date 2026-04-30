package my.management.module.tenant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.management.module.tenant.model.dto.TenantCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Server-side Tianditu geocoding adapter. The browser never receives the map key.
 */
@Service
public class TiandituGeocodeService {

    private static final Logger log = LoggerFactory.getLogger(TiandituGeocodeService.class);
    private static final String SUCCESS_STATUS = "0";
    private static final long DEFAULT_TIMEOUT_SECONDS = 5L;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${map.tianditu.geocode.enabled:false}")
    private boolean enabled;

    @Value("${map.tianditu.geocode.endpoint:https://api.tianditu.gov.cn/geocoder}")
    private String endpoint;

    @Value("${map.tianditu.geocode.key:}")
    private String apiKey;

    @Value("${map.tianditu.geocode.timeout-seconds:5}")
    private long timeoutSeconds;

    public TiandituGeocodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
    }

    public GeocodeResult resolve(TenantCreateRequest tenantRequest) {
        if (!enabled || isBlank(apiKey)) {
            return GeocodeResult.empty();
        }
        String keyword = buildKeyword(tenantRequest);
        if (isBlank(keyword)) {
            return GeocodeResult.empty();
        }

        try {
            String ds = objectMapper.writeValueAsString(Map.of("keyWord", keyword));
            URI uri = buildUri(ds);
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(safeTimeoutSeconds()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Tianditu geocode failed, httpStatus={}, keyword={}", response.statusCode(), keyword);
                return GeocodeResult.empty();
            }
            return parseResponse(response.body(), keyword);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Tianditu geocode interrupted, keyword={}", keyword, ex);
            return GeocodeResult.empty();
        } catch (Exception ex) {
            log.warn("Tianditu geocode failed, keyword={}", keyword, ex);
            return GeocodeResult.empty();
        }
    }

    private URI buildUri(String ds) {
        String separator = safeEndpoint().contains("?") ? "&" : "?";
        String query = "ds=" + URLEncoder.encode(ds, StandardCharsets.UTF_8)
                + "&tk=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8);
        return URI.create(safeEndpoint() + separator + query);
    }

    private GeocodeResult parseResponse(String body, String keyword) throws Exception {
        if (isBlank(body)) {
            return GeocodeResult.empty();
        }
        JsonNode root = objectMapper.readTree(body);
        String status = root.path("status").asText();
        if (!SUCCESS_STATUS.equals(status)) {
            String message = root.path("msg").asText(root.path("message").asText(""));
            log.warn("Tianditu geocode returned non-success status={}, message={}, keyword={}", status, message, keyword);
            return GeocodeResult.empty();
        }
        JsonNode location = root.path("location");
        Double longitude = parseDouble(location.path("lon").asText(null));
        Double latitude = parseDouble(location.path("lat").asText(null));
        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
            log.warn("Tianditu geocode returned invalid coordinate, keyword={}", keyword);
            return GeocodeResult.empty();
        }
        return new GeocodeResult(latitude, longitude, keyword);
    }

    private String buildKeyword(TenantCreateRequest request) {
        if (request == null || isBlank(request.getCompanyAddress())) {
            return null;
        }
        String city = trim(request.getCompanyCity());
        String address = trim(request.getCompanyAddress());
        if (isBlank(city)) {
            return address;
        }
        if (address.startsWith(city)) {
            return address;
        }
        return city + address;
    }

    private String safeEndpoint() {
        return isBlank(endpoint) ? "https://api.tianditu.gov.cn/geocoder" : endpoint.trim();
    }

    private long safeTimeoutSeconds() {
        return timeoutSeconds <= 0L || timeoutSeconds > 30L ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    private Double parseDouble(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isValidLatitude(Double value) {
        return value != null && value >= -90D && value <= 90D;
    }

    private boolean isValidLongitude(Double value) {
        return value != null && value >= -180D && value <= 180D;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record GeocodeResult(Double latitude, Double longitude, String keyword) {
        public static GeocodeResult empty() {
            return new GeocodeResult(null, null, null);
        }

        public boolean isValid() {
            return latitude != null && latitude >= -90D && latitude <= 90D
                    && longitude != null && longitude >= -180D && longitude <= 180D;
        }
    }
}
