package my.hive.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JdbcSystemEventPublisher implements SystemEventPublisher {

    private static final String INSERT_SQL = """
            INSERT INTO system_event (
              event_key, source_app, event_type, level, tenant_code, module, title, content,
              biz_type, biz_no, trace_id, detail_json, create_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SystemEventProperties properties;

    @Override
    public void publish(SystemEvent event) {
        if (event == null || !properties.isEnabled()) {
            return;
        }
        try {
            LocalDateTime occurTime = event.getOccurTime() == null ? LocalDateTime.now() : event.getOccurTime();
            jdbcTemplate.update(INSERT_SQL,
                    defaultText(event.getEventKey(), UUID.randomUUID().toString()),
                    defaultText(event.getSourceApp(), properties.getSourceApp()),
                    defaultText(event.getEventType(), "SYSTEM_EVENT"),
                    defaultText(event.getLevel(), "INFO"),
                    blankToNull(event.getTenantCode()),
                    blankToNull(event.getModule()),
                    truncate(defaultText(event.getTitle(), "System event"), properties.getMaxTitleLength()),
                    truncate(event.getContent(), properties.getMaxContentLength()),
                    blankToNull(event.getBizType()),
                    blankToNull(event.getBizNo()),
                    blankToNull(event.getTraceId()),
                    toJson(event.getDetail()),
                    occurTime);
        } catch (DataAccessException ex) {
            log.warn("system_event write failed, please confirm migration V20260429_005_system_event.sql has been executed", ex);
        } catch (Exception ex) {
            log.warn("system_event publish failed", ex);
        }
    }

    private String toJson(Object detail) throws Exception {
        if (detail == null) {
            return null;
        }
        return truncate(objectMapper.writeValueAsString(detail), properties.getMaxJsonLength());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
