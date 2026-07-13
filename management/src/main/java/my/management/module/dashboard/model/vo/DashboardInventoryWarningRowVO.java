package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * DashboardInventoryWarningRowVO 属于管理端后端总览大盘模块，定义出参结构。
 */
@Data
public class DashboardInventoryWarningRowVO {

    private String modelCode;

    private BigDecimal totalMeters;

    private LocalDateTime latestTime;
}
