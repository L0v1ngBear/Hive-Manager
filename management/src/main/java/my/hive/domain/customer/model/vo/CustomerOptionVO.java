package my.hive.domain.customer.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户选项对象，给订单等需要轻量联动的页面提供客户和项目候选项。
 */
@Data
public class CustomerOptionVO {

    private Long id;

    private String customerName;

    private String contactName;

    private String contactPhone;

    private String customerAddress;

    private LocalDate openingDate;

    private List<String> projectNames;
}
