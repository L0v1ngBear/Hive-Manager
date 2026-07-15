package my.hive.api;

import my.hive.HiveApplication;
import my.hive.domain.auth.model.vo.LoginVO;
import my.hive.domain.auth.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = HiveApplication.class,
        properties = {
                "spring.profiles.active=test",
                "spring.datasource.url=jdbc:h2:mem:hive-endpoint-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.task.scheduling.enabled=false",
                "xxl.job.enabled=false",
                "hive.system-event.enabled=false",
                "app.default-password.employee=Test@123456",
                "app.default-password.tenant-owner=Test@123456"
        }
)
@AutoConfigureMockMvc
class UnifiedEndpointSmokeTest {

    private static final String BUILD_HEADER = "X-Hive-Build";
    private static final String INSTANCE_HEADER = "X-Hive-Instance";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void adminAndMiniAuthenticationExposeTheSameBuildIdentityForSuccessAndRejection() throws Exception {
        LoginVO login = new LoginVO();
        login.setToken("test-token");
        login.setUserId(7L);
        login.setTenantCode("TENANT_001");
        when(authenticationService.adminLogin(any(), anyString())).thenReturn(login);
        when(authenticationService.miniLogin(any(), anyString())).thenReturn(login);

        MvcResult admin = mvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists(BUILD_HEADER))
                .andExpect(header().exists(INSTANCE_HEADER))
                .andReturn();
        MvcResult mini = mvc.perform(post("/auth/mini/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists(BUILD_HEADER))
                .andExpect(header().exists(INSTANCE_HEADER))
                .andReturn();
        MvcResult rejected = mvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(BUILD_HEADER))
                .andExpect(header().exists(INSTANCE_HEADER))
                .andReturn();

        String build = admin.getResponse().getHeader(BUILD_HEADER);
        assertThat(build).isNotBlank()
                .isEqualTo(mini.getResponse().getHeader(BUILD_HEADER))
                .isEqualTo(rejected.getResponse().getHeader(BUILD_HEADER));
        assertThat(admin.getResponse().getHeader(INSTANCE_HEADER)).isNotBlank()
                .isEqualTo(mini.getResponse().getHeader(INSTANCE_HEADER))
                .isEqualTo(rejected.getResponse().getHeader(INSTANCE_HEADER));
    }

    @Test
    void healthAndRepresentativeUnifiedRoutesAreRegisteredOnce() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(BUILD_HEADER))
                .andExpect(header().exists(INSTANCE_HEADER));

        Map<String, HttpMethod> routes = Map.ofEntries(
                Map.entry("/health", HttpMethod.GET),
                Map.entry("/auth/admin/login", HttpMethod.POST),
                Map.entry("/auth/mini/login", HttpMethod.POST),
                Map.entry("/auth/me", HttpMethod.GET),
                Map.entry("/emp/employee/page", HttpMethod.GET),
                Map.entry("/orders", HttpMethod.GET),
                Map.entry("/approval/summary", HttpMethod.GET),
                Map.entry("/inventory/summary", HttpMethod.GET),
                Map.entry("/notifications/page", HttpMethod.GET),
                Map.entry("/print-task/recent", HttpMethod.GET)
        );
        routes.forEach(this::assertSingleMapping);
    }

    private void assertSingleMapping(String path, HttpMethod method) {
        assertThat(mappings.getHandlerMethods().entrySet())
                .filteredOn(entry -> entry.getKey().getPatternValues().contains(path))
                .filteredOn(entry -> entry.getKey().getMethodsCondition().getMethods().stream()
                        .anyMatch(mappedMethod -> mappedMethod.name().equals(method.name())))
                .as(method + " " + path + " (relative to /api)")
                .hasSize(1);
    }
}
