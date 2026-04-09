package my.management.module.customer.model.dto;

import lombok.Data;

@Data
public class CustomerPageRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
}
