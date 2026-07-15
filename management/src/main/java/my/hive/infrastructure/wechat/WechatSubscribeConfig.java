package my.hive.infrastructure.wechat;

import java.util.List;

public record WechatSubscribeConfig(boolean enabled, String todoTemplateId, List<String> templateIds) {
}
