package my.management.module.badproduct.model.dto;

import lombok.Data;

/**
 * 次品分页查询请求。
 */
@Data
public class BadProductPageRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String status;

    private String type;

    private String date;
}
