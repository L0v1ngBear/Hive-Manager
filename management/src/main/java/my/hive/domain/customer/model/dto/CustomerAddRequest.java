package my.hive.domain.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.entity.CustomerProject;

import java.util.List;
/**
 * CustomerAddRequest 属于管理端后端客户模块，定义入参结构。
 */
@Data
public class CustomerAddRequest {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotNull(message = "客户类型不能为空")
    private Integer customerType;

    private List<CustomerContact> contacts;

    private List<CustomerProject> projects;
}
