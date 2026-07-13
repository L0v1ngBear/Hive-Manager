package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * DashboardPendingPrintRowVO 属于管理端后端总览大盘模块，定义出参结构。
 */
@Data
public class DashboardPendingPrintRowVO {

    private String orderNo;

    private String customerName;

    private LocalDateTime updateTime;
}
