package my.management.module.tenant.model.dto;

import lombok.Data;
/**
 * TenantPageRequest 属于管理端后端租户模块，定义入参结构。
 */
@Data
public class TenantPageRequest {

    private Long current = 1L;

    private Long size = 10L;

    private String keyword;

    private Integer status;
}
