package my.hive.domain.auth.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WechatTenantSelectionPayload {
    private String phoneHash;
    private List<String> tenantCodes = new ArrayList<>();
    private Long expireAt;
}
