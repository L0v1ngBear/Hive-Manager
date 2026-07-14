package my.hive.shared.print.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 打印任务展示对象。
 */
@Data
public class PrintTaskVO {

    private Long id;

    private String taskNo;

    private String printType;

    private String bizType;

    private String bizNo;

    private Integer status;

    private Integer retryCount;

    private String printChannel;

    private String deviceName;

    private String errorMessage;

    private Map<String, Object> printPayload;

    private LocalDateTime createTime;

    private LocalDateTime printedTime;
}
