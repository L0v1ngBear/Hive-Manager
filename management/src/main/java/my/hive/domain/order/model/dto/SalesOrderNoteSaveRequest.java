package my.hive.domain.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalesOrderNoteSaveRequest {

    private Long id;

    @NotBlank(message = "备注内容不能为空")
    @Size(max = 1000, message = "单条备注不能超过1000字")
    private String content;

    private Integer version;
}
