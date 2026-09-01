package my.hive.domain.aftersales.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AfterSalesRepairImageRequest {

    @NotBlank(message = "维修图片名称不能为空")
    @Size(max = 255, message = "维修图片名称不能超过255个字符")
    private String fileName;

    @NotBlank(message = "维修图片地址不能为空")
    @Size(max = 500, message = "维修图片地址不能超过500个字符")
    private String fileUrl;

    @PositiveOrZero(message = "维修图片大小不能为负数")
    private Long fileSize;
}
