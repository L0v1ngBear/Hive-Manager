package my.hive.api.auth;

import my.hive.domain.auth.model.vo.LoginVO;
import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.config.WebMvcConfig;
import my.hive.shared.interceptor.PlatformScopeInterceptor;
import my.hive.shared.interceptor.TenantContextFilter;
import my.hive.shared.utils.ResponseEncryptUtil;
import my.hive.shared.web.TrustedClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MiniAuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = my.hive.shared.advice.ResultEncryptAdvice.class))
@ContextConfiguration(classes = {MiniAuthController.class, WebMvcConfig.class})
class WechatTenantSelectionPublicPathIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuthenticationService authenticationService;

    @MockBean
    TenantContextFilter tenantContextFilter;

    @MockBean
    PlatformScopeInterceptor platformScopeInterceptor;

    @MockBean
    ResponseEncryptUtil responseEncryptUtil;

    @MockBean
    TrustedClientIpResolver trustedClientIpResolver;

    @Test
    void selectionRouteReachesControllerWithoutSessionInterceptors() throws Exception {
        LoginVO login = new LoginVO();
        login.setUserId(7L);
        login.setTenantCode("tenant-a");
        when(trustedClientIpResolver.resolve(any())).thenReturn("203.0.113.9");
        when(authenticationService.selectWechatTenant(any(), org.mockito.ArgumentMatchers.eq("203.0.113.9"))).thenReturn(login);

        mvc.perform(post("/auth/mini/wechat-login/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectionTicket\":\"ticket\",\"tenantCode\":\"tenant-a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7));

        verifyNoInteractions(tenantContextFilter, platformScopeInterceptor);
    }
}
