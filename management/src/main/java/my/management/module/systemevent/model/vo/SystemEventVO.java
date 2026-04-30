package my.management.module.systemevent.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemEventVO {

    private Long id;

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

    private String detailJson;

    private Integer handled;

    private String handledBy;

    private LocalDateTime handledTime;

    private LocalDateTime createTime;
}
