package my.management.module.price.model.dto;

import lombok.Data;

@Data
public class PricePageRequest {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private Integer status;
}
