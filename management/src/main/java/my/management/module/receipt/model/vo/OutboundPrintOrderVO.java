package my.management.module.receipt.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * OutboundPrintOrderVO 属于管理端后端打印回执模块，定义出参结构。
 */
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
