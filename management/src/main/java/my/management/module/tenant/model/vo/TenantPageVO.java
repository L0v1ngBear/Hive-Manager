package my.management.module.tenant.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
/**
 * TenantPageVO 属于管理端后端租户模块，定义出参结构。
 */
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
