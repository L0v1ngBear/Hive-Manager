package my.hive.domain.order.model.dto;

import lombok.Data;

@Data
public class SalesOrderShipmentSaveRequest {

    private Long id;

    private String logisticsCompany;

    private String trackingNo;

    private String deliveryMode;

    private Integer version;
}
