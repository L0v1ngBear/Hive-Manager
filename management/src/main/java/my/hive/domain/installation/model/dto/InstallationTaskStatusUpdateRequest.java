package my.hive.domain.installation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InstallationTaskStatusUpdateRequest {

    @NotNull(message = "安装任务不能为空")
    private Long id;

    @NotBlank(message = "安装任务状态不能为空")
    private String status;

    private String expressCompany;

    private String expressNo;

    private List<InstallationTaskInstallerRequest> installers;

    private String constructionRemark;

    private String specialExceptionNote;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;
}
