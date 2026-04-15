package my.management.module.dashboard.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardPendingPrintRowVO {

    private String orderNo;

    private String customerName;

    private LocalDateTime updateTime;
}
