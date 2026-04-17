package my.management.module.price.model.dto;

import lombok.Data;
/**
 * PricePageRequest 属于管理端后端价格模块，定义入参结构。
 */
@Data
public class PricePageRequest {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private Integer status;
}
