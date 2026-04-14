package my.management.module.tenant.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantPageVO {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private Integer tenantType;

    private String contactPerson;

    private String contactPhone;

    private Integer status;

    private LocalDateTime createTime;
}
