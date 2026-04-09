package my.management.module.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import my.management.module.customer.model.entity.CustomerContact;
import my.management.module.customer.model.entity.CustomerProject;

import java.util.List;

@Data
public class CustomerAddRequest {

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotNull(message = "customerType is required")
    private Integer customerType;

    @NotBlank(message = "constructionArea is required")
    private String constructionArea;

    private List<CustomerContact> contacts;

    private List<CustomerProject> projects;
}
