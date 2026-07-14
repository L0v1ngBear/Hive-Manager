package my.hive.api.auth;

import my.hive.domain.auth.service.AuthenticationService;
import my.hive.shared.utils.ResponseEncryptUtil;
import my.management.common.interceptor.PlatformScopeInterceptor;
import my.management.common.interceptor.TenantContextFilter;
import my.management.module.auth.model.vo.LoginVO;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminAuthController.class, MiniAuthController.class, SessionController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = my.management.common.config.WebMvcConfig.class))
@ContextConfiguration(classes = {AdminAuthController.class, MiniAuthController.class, SessionController.class})
class UnifiedAuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean AuthenticationService authenticationService;
    @MockBean ResponseEncryptUtil responseEncryptUtil;
    @MockBean TenantContextFilter tenantContextFilter;
    @MockBean PlatformScopeInterceptor platformScopeInterceptor;

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

    @Test void legacyAuthenticationServiceImplementationIsRemoved() {
        Path legacy = Path.of("src/main/java/my/management/module/auth/service/AuthService.java");
        Path canonical = Path.of("src/main/java/my/hive/domain/auth/service/AuthenticationService.java");
        org.assertj.core.api.Assertions.assertThat(Files.exists(legacy)).isFalse();
        org.assertj.core.api.Assertions.assertThat(Files.exists(canonical)).isTrue();
    }
}
