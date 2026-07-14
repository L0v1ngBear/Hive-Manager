package my.hive.domain.customer.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * CustomerUpdateRequest 属于管理端后端客户模块，定义入参结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerUpdateRequest extends CustomerAddRequest {

    @NotNull(message = "id is required")
    private Long id;
}
