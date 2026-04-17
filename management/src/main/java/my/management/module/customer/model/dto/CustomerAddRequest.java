package my.management.module.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import my.management.module.customer.model.entity.CustomerContact;
import my.management.module.customer.model.entity.CustomerProject;

import java.util.List;
/**
 * CustomerAddRequest 属于管理端后端客户模块，定义入参结构。
 */
@Data
public class CustomerAddRequest {

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotNull(message = "customerType is required")
    private Integer customerType;

    private List<CustomerContact> contacts;

    private List<CustomerProject> projects;
}
