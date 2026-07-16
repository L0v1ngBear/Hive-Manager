package my.hive.domain.installation.model.vo;

import lombok.Data;

@Data
public class InstallationTaskInstallerVO {

    private Long id;

    private String name;

    private String phone;

    private Integer sortOrder;
}
