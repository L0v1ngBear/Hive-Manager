package my.management.module.tenant.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantOwnerAccountRequest {

    /**
     * 为空时按登录账号或手机号定位；有值时优先重新分配给该租户下的指定用户。
     */
    private Long ownerUserId;

    @NotBlank(message = "负责人姓名不能为空")
    private String ownerName;

    /**
     * 交付给客户老板的网页登录账号；为空时使用手机号作为账号。
     */
    private String loginName;

    /**
     * 完整手机号。重新分配给已有账号时可留空，系统会沿用该账号原手机号。
     */
    private String phone;

    @NotBlank(message = "初始密码不能为空")
    private String initialPassword;

    /**
     * 老板账号默认不需要打卡，需要时可显式开启。
     */
    private Boolean attendanceRequired = false;
}
