package my.hive.domain.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationJoinCodeSendRequest {

    @NotBlank(message = "请输入手机号")
    private String phone;
}
