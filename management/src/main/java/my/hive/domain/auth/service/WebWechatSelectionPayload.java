package my.hive.domain.auth.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class WebWechatSelectionPayload {
    private String subjectHash;
    private List<String> tenantCodes = new ArrayList<>();
    private Map<String, Long> verifiedUserIds = new LinkedHashMap<>();
    private boolean bindOnSelect;
    private Long expireAt;
}
