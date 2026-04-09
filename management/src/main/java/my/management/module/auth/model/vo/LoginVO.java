package my.management.module.auth.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String userName;

    private String tenantCode;

    private List<String> permissions = new ArrayList<>();
}