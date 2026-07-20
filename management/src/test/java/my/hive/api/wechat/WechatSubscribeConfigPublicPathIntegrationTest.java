package my.hive.api.wechat;

import my.hive.infrastructure.wechat.WechatSubscribeConfig;
import my.hive.infrastructure.wechat.WechatSubscribeService;
import my.hive.shared.config.WebMvcConfig;
import my.hive.shared.interceptor.PlatformScopeInterceptor;
import my.hive.shared.interceptor.TenantContextFilter;
import my.hive.shared.utils.ResponseEncryptUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebMvcTest(controllers = WechatSubscribeController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = my.hive.shared.advice.ResultEncryptAdvice.class))
@ContextConfiguration(classes = {WechatSubscribeController.class, WebMvcConfig.class})
class WechatSubscribeConfigPublicPathIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    WechatSubscribeService subscribeService;

    @MockBean
    TenantContextFilter tenantContextFilter;

    @MockBean
    PlatformScopeInterceptor platformScopeInterceptor;

    @MockBean
    ResponseEncryptUtil responseEncryptUtil;

    @Test
    void configRouteReachesControllerWithoutSessionInterceptors() throws Exception {
        when(subscribeService.config()).thenReturn(new WechatSubscribeConfig(false, null, List.of()));

        mvc.perform(get("/wechat/subscriptions/config"))
                .andExpect(status().isOk());

        verifyNoInteractions(tenantContextFilter, platformScopeInterceptor);
    }

    @Test
    void registerRouteStillRequiresSessionInterceptors() throws Exception {
        when(tenantContextFilter.preHandle(any(), any(), any())).thenAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401);
            return false;
        });

        mvc.perform(post("/wechat/subscriptions/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "wechat-code",
                                  "subscriptions": [
                                    {
                                      "templateId": "template-id",
                                      "status": "accept"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(tenantContextFilter).preHandle(any(), any(), any());
        verify(subscribeService, never()).register(any());
        verifyNoInteractions(platformScopeInterceptor);
    }
}
