package my.management.module.tenant.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * TenantCreateRequest 属于管理端后端租户模块，定义入参结构。
 */
@Data
public class TenantCreateRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    private Integer tenantType = 1;

    private String contactPerson;

    private String contactPhone;

    private String password;

    @Size(max = 255, message = "公司地址长度不能超过255个字符")
    private String companyAddress;

    @Size(max = 50, message = "城市名称长度不能超过50个字符")
    private String companyCity;

    private Double companyLatitude;

    private Double companyLongitude;

    private Double attendanceRadius;
}
