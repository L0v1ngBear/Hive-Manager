package my.management.module.customer.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerUpdateRequest extends CustomerAddRequest {

    @NotNull(message = "id is required")
    private Long id;
}
