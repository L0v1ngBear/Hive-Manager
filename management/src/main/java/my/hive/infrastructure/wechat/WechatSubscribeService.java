package my.hive.infrastructure.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatSubscribeService {

    private static final Set<String> ALLOWED_STATUS = Set.of("accept", "reject", "ban");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WechatMiniProgramClient client;
    private final my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper mapper;

    @Value("${wechat.mini-program.enabled:false}")
    private boolean wechatEnabled;
    @Value("${wechat.mini-program.subscribe.enabled:false}")
    private boolean subscribeEnabled;
    @Value("${wechat.mini-program.subscribe.todo-template-id:}")
    private String todoTemplateId;
    @Value("${wechat.mini-program.subscribe.todo-title-key:thing1}")
    private String titleKey;
    @Value("${wechat.mini-program.subscribe.todo-content-key:thing2}")
    private String contentKey;
    @Value("${wechat.mini-program.subscribe.todo-time-key:time3}")
    private String timeKey;

    public WechatSubscribeConfig config() {
        boolean enabled = ready();
        return new WechatSubscribeConfig(enabled, enabled ? todoTemplateId.trim() : null,
                enabled ? List.of(todoTemplateId.trim()) : List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(WechatSubscribeRegisterRequest request) {
        requireReady();
        String tenantCode = requireTenantCode();
        Long userId = requireUserId();
        String openId = client.exchangeOpenId(request.getCode().trim());
        for (WechatSubscribeRegisterRequest.TemplateSubscription subscription : request.getSubscriptions()) {
            String templateId = subscription.getTemplateId().trim();
            if (!templateId.equals(todoTemplateId.trim())) {
                throw new BusinessException("订阅模板与系统配置不一致");
            }
            String status = subscription.getStatus().trim().toLowerCase();
            if (!ALLOWED_STATUS.contains(status)) {
                throw new BusinessException("无效的微信订阅授权状态");
            }
            WechatSubscribeUser entity = mapper.selectOne(new LambdaQueryWrapper<WechatSubscribeUser>()
                    .eq(WechatSubscribeUser::getTenantCode, tenantCode)
                    .eq(WechatSubscribeUser::getUserId, userId)
                    .eq(WechatSubscribeUser::getTemplateId, templateId)
                    .last("LIMIT 1"));
            if (entity == null) {
                entity = new WechatSubscribeUser();
                entity.setTenantCode(tenantCode);
                entity.setUserId(userId);
                entity.setTemplateId(templateId);
            }
            entity.setOpenid(openId);
            entity.setSubscribeStatus(status);
            if (entity.getId() == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
        }
    }

    public void sendTodoAfterCommit(Long userId, String title, String content, String page) {
        if (userId == null) {
            return;
        }
        Runnable send = () -> safeSend(userId, title, content, page);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    boolean sendTodo(Long userId, String title, String content, String page) {
        if (!ready()) {
            return false;
        }
        String tenantCode = requireTenantCode();
        WechatSubscribeUser subscription = mapper.selectOne(new LambdaQueryWrapper<WechatSubscribeUser>()
                .eq(WechatSubscribeUser::getTenantCode, tenantCode)
                .eq(WechatSubscribeUser::getUserId, userId)
                .eq(WechatSubscribeUser::getTemplateId, todoTemplateId.trim())
                .eq(WechatSubscribeUser::getSubscribeStatus, "accept")
                .last("LIMIT 1"));
        if (subscription == null || subscription.getOpenid() == null || subscription.getOpenid().isBlank()) {
            return false;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(titleKey.trim(), limit(title, 20));
        fields.put(contentKey.trim(), limit(content, 20));
        fields.put(timeKey.trim(), LocalDateTime.now().format(TIME_FORMAT));
        boolean sent = client.sendSubscribeMessage(subscription.getOpenid(), todoTemplateId.trim(), page, fields);
        if (sent) {
            subscription.setSubscribeStatus("used");
            mapper.updateById(subscription);
        }
        return sent;
    }

    private void safeSend(Long userId, String title, String content, String page) {
        try {
            sendTodo(userId, title, content, page);
        } catch (Exception exception) {
            log.warn("wechat subscription message failed without rolling back business transaction, userId={}", userId, exception);
        }
    }

    private boolean ready() {
        return wechatEnabled && subscribeEnabled && todoTemplateId != null && !todoTemplateId.isBlank();
    }

    private void requireReady() {
        if (!ready()) {
            throw new BusinessException(503, "微信订阅消息未启用");
        }
    }

    private String requireTenantCode() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(401, "登录租户上下文已失效");
        }
        return tenantCode;
    }

    private Long requireUserId() {
        Long userId = TenantPermissionContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "登录用户上下文已失效");
        }
        return userId;
    }

    private String limit(String value, int maxLength) {
        String text = value == null || value.isBlank() ? "-" : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
