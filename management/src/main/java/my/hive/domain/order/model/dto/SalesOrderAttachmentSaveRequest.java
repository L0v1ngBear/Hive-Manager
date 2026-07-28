package my.hive.domain.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalesOrderAttachmentSaveRequest {

    @NotBlank(message = "附件名称不能为空")
    @Size(max = 255, message = "附件名称不能超过255个字符")
    private String fileName;

    @NotBlank(message = "附件地址不能为空")
    @Size(max = 500, message = "附件地址不能超过500个字符")
    private String fileUrl;

    @PositiveOrZero(message = "附件大小不能为负数")
    private Long fileSize;
}
