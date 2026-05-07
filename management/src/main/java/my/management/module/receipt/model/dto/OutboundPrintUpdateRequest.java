package my.management.module.receipt.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 出库单打印前人工修正入参。保存后会回写出库单并记录修正快照。
 */
@Data
public class OutboundPrintUpdateRequest {

    private Long id;

    private String orderNo;

    private String customerName;

    private String projectName;

    private LocalDate printDate;

    private String operator;

    private String logisticsCompany;

    private String logisticsNo;

    private String editReason;

    private List<OutboundPrintItemUpdateRequest> items = new ArrayList<>();
}
