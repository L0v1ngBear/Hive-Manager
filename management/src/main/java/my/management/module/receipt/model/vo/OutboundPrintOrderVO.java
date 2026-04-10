package my.management.module.receipt.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OutboundPrintOrderVO {

    private Long id;

    private String orderNo;

    private String customerName;

    private Integer itemCount;

    private Float totalMeters;

    private String operator;

    private LocalDateTime createTime;
}