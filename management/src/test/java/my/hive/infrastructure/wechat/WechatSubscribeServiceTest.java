package my.hive.infrastructure.wechat;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatSubscribeServiceTest {

    @AfterEach
    void clearContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void acceptedSubscriptionIsStoredForTheSharedTenantAndUser() {
        WechatMiniProgramClient client = mock(WechatMiniProgramClient.class);
        my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper mapper = mock(my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper.class);
        WechatSubscribeService service = configuredService(client, mapper);
        TenantPermissionContext.init("TENANT_A", 7L, Set.of());
        when(client.exchangeOpenId("login-code")).thenReturn("openid-1");
        when(mapper.insert(any(WechatSubscribeUser.class))).thenAnswer(invocation -> {
            WechatSubscribeUser entity = invocation.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(entity.getTenantCode()).isEqualTo("TENANT_A");
            org.assertj.core.api.Assertions.assertThat(entity.getUserId()).isEqualTo(7L);
            org.assertj.core.api.Assertions.assertThat(entity.getSubscribeStatus()).isEqualTo("accept");
            return 1;
        });

        service.register(request("todo-template", "accept"));

        verify(mapper).insert(any(WechatSubscribeUser.class));
    }

    @Test
    void registrationRejectsAnUnconfiguredTemplate() {
        WechatMiniProgramClient client = mock(WechatMiniProgramClient.class);
        my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper mapper = mock(my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper.class);
        WechatSubscribeService service = configuredService(client, mapper);
        TenantPermissionContext.init("TENANT_A", 7L, Set.of());
        when(client.exchangeOpenId("login-code")).thenReturn("openid-1");

        assertThatThrownBy(() -> service.register(request("other-template", "accept")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订阅模板");
    }

    private WechatSubscribeService configuredService(WechatMiniProgramClient client,
                                                     my.hive.infrastructure.wechat.mapper.WechatSubscribeUserMapper mapper) {
        WechatSubscribeService service = new WechatSubscribeService(client, mapper);
        ReflectionTestUtils.setField(service, "wechatEnabled", true);
        ReflectionTestUtils.setField(service, "subscribeEnabled", true);
        ReflectionTestUtils.setField(service, "todoTemplateId", "todo-template");
        ReflectionTestUtils.setField(service, "titleKey", "thing1");
        ReflectionTestUtils.setField(service, "contentKey", "thing2");
        ReflectionTestUtils.setField(service, "timeKey", "time3");
        return service;
    }

    private WechatSubscribeRegisterRequest request(String templateId, String status) {
        WechatSubscribeRegisterRequest.TemplateSubscription subscription =
                new WechatSubscribeRegisterRequest.TemplateSubscription();
        subscription.setTemplateId(templateId);
        subscription.setStatus(status);
        WechatSubscribeRegisterRequest request = new WechatSubscribeRegisterRequest();
        request.setCode("login-code");
        request.setSubscriptions(List.of(subscription));
        return request;
    }
}
