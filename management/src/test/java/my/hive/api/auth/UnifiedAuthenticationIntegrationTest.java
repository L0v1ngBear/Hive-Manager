package my.hive.api.auth;

import my.hive.domain.auth.service.AuthenticationService;
import my.hive.domain.auth.model.vo.MiniWechatLoginVO;
import my.hive.domain.auth.model.vo.WebWechatConfigVO;
import my.hive.domain.auth.model.vo.WebWechatLoginVO;
import my.hive.domain.auth.model.vo.WebWechatSessionVO;
import my.hive.shared.utils.ResponseEncryptUtil;
import my.hive.shared.interceptor.PlatformScopeInterceptor;
import my.hive.shared.interceptor.TenantContextFilter;
import my.hive.shared.web.TrustedClientIpResolver;
import my.hive.domain.auth.model.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminAuthController.class, MiniAuthController.class, SessionController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = my.hive.shared.config.WebMvcConfig.class))
@ContextConfiguration(classes = {AdminAuthController.class, MiniAuthController.class, SessionController.class})
class UnifiedAuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean AuthenticationService authenticationService;
    @MockBean ResponseEncryptUtil responseEncryptUtil;
    @MockBean TenantContextFilter tenantContextFilter;
    @MockBean PlatformScopeInterceptor platformScopeInterceptor;
    @MockBean TrustedClientIpResolver trustedClientIpResolver;

    @BeforeEach
    void setUpClientIp() {
        when(trustedClientIpResolver.resolve(any())).thenReturn("127.0.0.1");
    }

    @Test void exposesSeparateAdminAndMiniLoginAdaptersWithOneResponseShape() throws Exception {
        LoginVO login = new LoginVO();
        login.setToken("token");
        login.setUserId(7L);
        login.setTenantCode("tenant-a");
        when(authenticationService.adminLogin(any(), anyString())).thenReturn(login);
        when(authenticationService.miniLogin(any(), anyString())).thenReturn(login);

        mvc.perform(post("/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").value("token"));
        mvc.perform(post("/auth/mini/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").value("token"));
    }

    @Test void exposesAllApprovedRelativeMappingsAndNoAmbiguousLogin() throws Exception {
        when(authenticationService.createScanLoginSession()).thenReturn(null);
        when(authenticationService.scanLoginStatus(anyString())).thenReturn(null);
        mvc.perform(post("/auth/admin/scan-login/session")).andExpect(status().isOk());
        mvc.perform(get("/auth/admin/scan-login/status").param("sceneKey", "scene")).andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test void exposesAuthenticatedPasswordChangeRoute() throws Exception {
        mvc.perform(post("/auth/admin/password").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"OldPass1","newPassword":"NewPass2","confirmPassword":"NewPass2"}
                                """))
                .andExpect(status().isOk());
        verify(authenticationService).changePassword(any());
    }

    @Test void legacyAuthenticationServiceImplementationIsRemoved() {
        Path legacy = Path.of("src/main/java/my/management/module/auth/service/AuthService.java");
        Path canonical = Path.of("src/main/java/my/hive/domain/auth/service/AuthenticationService.java");
        org.assertj.core.api.Assertions.assertThat(Files.exists(legacy)).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.exists(canonical)).isTrue();
    }

    @Test void exposesWechatLoginAndTenantSelectionRoutes() throws Exception {
        LoginVO principal=new LoginVO(); principal.setUserId(7L); principal.setTenantCode("tenant-a");
        MiniWechatLoginVO wechatLogin = new MiniWechatLoginVO();
        wechatLogin.setFlowStatus("LOGGED_IN");
        wechatLogin.setLoginInfo(principal);
        when(trustedClientIpResolver.resolve(any())).thenReturn("203.0.113.9");
        when(authenticationService.wechatLogin(any(), anyString())).thenReturn(wechatLogin);
        when(authenticationService.selectWechatTenant(any(), anyString())).thenReturn(principal);
        mvc.perform(post("/auth/mini/wechat-login").contentType(MediaType.APPLICATION_JSON).content("{\"phoneCode\":\"wx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowStatus").value("LOGGED_IN"))
                .andExpect(jsonPath("$.data.loginInfo.userId").value(7));
        mvc.perform(post("/auth/mini/wechat-login/select").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectionTicket\":\"ticket\",\"tenantCode\":\"tenant-a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7));
        verify(authenticationService).wechatLogin(any(), eq("203.0.113.9"));
        verify(authenticationService).selectWechatTenant(any(), eq("203.0.113.9"));
    }

    @Test void exposesWebWechatLoginSessionCompletionBindingAndSelectionRoutes() throws Exception {
        LoginVO principal = new LoginVO();
        principal.setToken("token");
        principal.setUserId(7L);
        principal.setTenantCode("tenant-a");
        WebWechatSessionVO session = new WebWechatSessionVO();
        session.setAuthorizationUrl("https://open.weixin.qq.com/connect/qrconnect?state=state");
        WebWechatLoginVO flow = new WebWechatLoginVO();
        flow.setFlowStatus("LOGGED_IN");
        flow.setLoginInfo(principal);
        when(authenticationService.webWechatLoginConfig()).thenReturn(new WebWechatConfigVO(true));
        when(authenticationService.createWebWechatLoginSession(anyString())).thenReturn(session);
        when(authenticationService.completeWebWechatLogin(any(), anyString())).thenReturn(flow);
        when(authenticationService.bindWebWechatLogin(any(), anyString())).thenReturn(flow);
        when(authenticationService.selectWebWechatTenant(any(), anyString())).thenReturn(principal);

        mvc.perform(get("/auth/admin/wechat-login/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
        mvc.perform(post("/auth/admin/wechat-login/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl").value(session.getAuthorizationUrl()));
        mvc.perform(post("/auth/admin/wechat-login/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginTicket\":\"ticket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginInfo.token").value("token"));
        mvc.perform(post("/auth/admin/wechat-login/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingTicket\":\"bind\",\"username\":\"alice\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowStatus").value("LOGGED_IN"));
        mvc.perform(post("/auth/admin/wechat-login/select").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectionTicket\":\"select\",\"tenantCode\":\"tenant-a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantCode").value("tenant-a"));
    }

    @Test void exposesScanConfirmMeAndLogoutThroughSharedService() throws Exception {
        LoginVO principal=new LoginVO(); principal.setUserId(7L); principal.setTenantCode("tenant-a");
        when(authenticationService.currentUser()).thenReturn(principal);
        mvc.perform(post("/auth/admin/scan-login/confirm").contentType(MediaType.APPLICATION_JSON).content("{\"sceneKey\":\"scene\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.data.tenantCode").value("tenant-a"));
        mvc.perform(post("/auth/logout")).andExpect(status().isOk());
        verify(authenticationService).confirmWebScanLogin(any()); verify(authenticationService).logout();
    }
}
