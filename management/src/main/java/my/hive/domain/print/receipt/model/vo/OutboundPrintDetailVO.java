package my.hive.domain.print.receipt.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * OutboundPrintDetailVO 属于管理端后端打印回执模块，定义出参结构。
 */
@Data
public class OutboundPrintDetailVO {

    private Long id;

    private String orderNo;

    private String customerName;

    private String projectName;

    private String operator;

    private LocalDate printDate;

    private String logisticsCompany;

    private String logisticsNo;

    private Integer printEditCount;

    private LocalDateTime createTime;

    private Float totalMeters;

    private BigDecimal totalAmount;

    private List<OutboundPrintItemVO> items = new ArrayList<>();
}
