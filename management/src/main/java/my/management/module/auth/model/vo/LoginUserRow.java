package my.management.module.auth.model.vo;

import lombok.Data;
/**
 * LoginUserRow 属于管理端后端认证模块，定义出参结构。
 */
@Data
public class LoginUserRow {

    private Long userId;

    private String tenantCode;

    private String tenantName;

    private String userName;

    private String loginName;

    private String phone;

    private String password;

    private Integer mustChangePassword;

    private Integer userStatus;
}
