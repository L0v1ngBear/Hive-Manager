package my.management.module.receipt.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OutboundPrintDetailVO {

    private Long id;

    private String orderNo;

    private String customerName;

    private String operator;

    private LocalDateTime createTime;

    private Float totalMeters;

    private BigDecimal totalAmount;

    private List<OutboundPrintItemVO> items = new ArrayList<>();
}