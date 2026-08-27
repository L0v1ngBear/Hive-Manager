package my.hive.shared.event;

import java.util.Map;

public interface SystemEventPublisher {

    void publish(SystemEvent event);

    default void info(String eventType, String title, String content, Object detail) {
        publish(SystemEvent.builder()
                .eventType(eventType)
                .level("INFO")
                .title(title)
                .content(content)
                .detail(detail)
                .build());
    }

    default void warn(String eventType, String title, String content, Object detail) {
        publish(SystemEvent.builder()
                .eventType(eventType)
                .level("WARN")
                .title(title)
                .content(content)
                .detail(detail)
                .build());
    }

    default void error(String eventType, String title, Throwable throwable, Object detail) {
        publish(SystemEvent.builder()
                .eventType(eventType)
                .level("ERROR")
                .title(title)
                // Exception messages can contain provider responses, SQL text,
                // credentials, or user supplied values.  Keep operational
                // events useful without making them another sensitive-data log.
                .content("系统处理失败，请查看受控服务器日志")
                .detail(detail == null && throwable != null
                        ? Map.of("errorType", throwable.getClass().getName())
                        : detail)
                .build());
    }
}
