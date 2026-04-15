package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DashboardInventoryWarningRowVO {

    private String modelCode;

    private BigDecimal totalMeters;

    private LocalDateTime latestTime;
}
