package my.management.module.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationJoinRequest {

    @NotBlank(message = "请输入姓名")
    private String name;

    @NotBlank(message = "请输入手机号")
    private String phone;

    @NotBlank(message = "请输入短信验证码")
    private String smsCode;

    @NotBlank(message = "请输入组织码")
    private String organizationCode;

    @NotBlank(message = "请设置登录密码")
    private String password;

    @NotBlank(message = "请再次输入登录密码")
    private String confirmPassword;
}
