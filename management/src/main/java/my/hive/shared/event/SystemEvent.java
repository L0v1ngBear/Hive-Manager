package my.hive.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent {

    private String eventKey;

    private String sourceApp;

    private String eventType;

    private String level;

    private String tenantCode;

    private String module;

    private String title;

    private String content;

    private String bizType;

    private String bizNo;

    private String traceId;

    private Object detail;

    private LocalDateTime occurTime;
}
