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
        String message = throwable == null ? null : throwable.getMessage();
        publish(SystemEvent.builder()
                .eventType(eventType)
                .level("ERROR")
                .title(title)
                .content(message)
                .detail(detail == null && throwable != null
                        ? Map.of("errorType", throwable.getClass().getName(), "errorMessage", message == null ? "" : message)
                        : detail)
                .build());
    }
}
