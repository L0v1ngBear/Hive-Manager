package my.management.module.customer.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * CustomerUpdateRequest 属于管理端后端客户模块，定义入参结构。
 */
@Data
public class CustomerUpdateRequest extends CustomerAddRequest {

    @NotNull(message = "id is required")
    private Long id;
}
