package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DashboardInventoryTrendRowVO {

    private LocalDateTime statDate;

    private BigDecimal dayInMeters;

    private BigDecimal dayOutMeters;
}
