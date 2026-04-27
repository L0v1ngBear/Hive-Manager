package my.management.module.operationlog.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运维日志分页查询条件，仅平台 super 使用。
 */
@Data
public class OperationLogPageRequest {

    private Long current = 1L;

    private Long size = 20L;

    private String keyword;

    private String logLevel;

    private String module;

    private Integer success;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
