package my.hive.domain.order.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesOrderShipmentVO {

    private Long id;
    private String logisticsCompany;
    private String trackingNo;
    private Integer sortOrder;
    private Integer version;
    private String creator;
    private String updater;
    private String updaterName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
