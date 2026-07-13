package my.management.module.auth.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * LoginVO 属于管理端后端认证模块，定义出参结构。
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String userName;

    private String tenantCode;

    private String tenantName;

    private String tenantLogoUrl;

    private Boolean developer = false;

    private Boolean mustChangePassword = false;

    private String responseKey;

    private List<String> permissions = new ArrayList<>();

    private List<String> features = new ArrayList<>();
}
