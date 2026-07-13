package my.management.module.tenant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantFeatureOptionVO {

    private String code;

    private String name;

    private String category;

    private String description;

    private Boolean baseModule;

    private Boolean defaultEnabled;
}
