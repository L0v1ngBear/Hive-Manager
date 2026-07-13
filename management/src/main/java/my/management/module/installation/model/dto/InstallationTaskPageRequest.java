package my.management.module.installation.model.dto;

import lombok.Data;

@Data
public class InstallationTaskPageRequest {

    private Long current = 1L;

    private Long size = 10L;

    private String status;

    private String keyword;

    private String customerName;

    private String projectName;
}
