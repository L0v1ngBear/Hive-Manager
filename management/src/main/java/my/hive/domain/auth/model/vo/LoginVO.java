package my.hive.domain.auth.model.vo;

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

    /** 员工档案职位（user.position），与员工管理列表同一事实；未设置时为 null。 */
    private String positionName;

    private String tenantCode;

    private String tenantName;

    private String tenantLogoUrl;

    private Boolean developer = false;

    private Boolean mustChangePassword = false;

    private String responseKey;

    private List<String> permissions = new ArrayList<>();

    private List<String> features = new ArrayList<>();
}
