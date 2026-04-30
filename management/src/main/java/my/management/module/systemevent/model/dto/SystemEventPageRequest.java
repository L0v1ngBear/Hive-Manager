package my.management.module.systemevent.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemEventPageRequest {

    private Long current = 1L;

    private Long size = 20L;

    private String keyword;

    private String level;

    private String sourceApp;

    private String eventType;

    private String module;

    private String tenantCode;

    private Integer handled;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
