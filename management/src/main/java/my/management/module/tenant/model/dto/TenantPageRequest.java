package my.management.module.tenant.model.dto;

import lombok.Data;

@Data
public class TenantPageRequest {

    private Long current = 1L;

    private Long size = 10L;

    private String keyword;

    private Integer status;
}
