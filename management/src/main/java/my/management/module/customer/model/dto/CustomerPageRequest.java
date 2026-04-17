package my.management.module.customer.model.dto;

import lombok.Data;
/**
 * CustomerPageRequest 属于管理端后端客户模块，定义入参结构。
 */
@Data
public class CustomerPageRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
}
