package my.hive.shared.print.dto;

import lombok.Data;

/**
 * 打印任务结果回传请求。
 */
@Data
public class PrintTaskReportRequest {

    private String taskNo;

    /** 1-成功，2-失败，3-作废 */
    private Integer status;

    private String printChannel;

    private String deviceName;

    private String errorMessage;
}
