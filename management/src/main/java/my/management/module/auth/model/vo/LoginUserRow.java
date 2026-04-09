package my.management.module.auth.model.vo;

import lombok.Data;

@Data
public class LoginUserRow {

    private Long userId;

    private String tenantCode;

    private String userName;

    private String loginName;

    private String phone;

    private String password;

    private Integer userStatus;
}