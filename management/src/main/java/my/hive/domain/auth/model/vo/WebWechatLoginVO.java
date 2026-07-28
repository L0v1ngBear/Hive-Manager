package my.hive.domain.auth.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WebWechatLoginVO {
    private String flowStatus;
    private LoginVO loginInfo;
    private String bindingTicket;
    private String selectionTicket;
    private List<WechatTenantOptionVO> tenants = new ArrayList<>();
}
