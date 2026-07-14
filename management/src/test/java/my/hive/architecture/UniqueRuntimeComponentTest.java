package my.hive.architecture;

import my.hive.HiveApplication;
import my.management.controller.AuthController;
import my.management.controller.OrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = HiveApplication.class,
        properties = {
                "spring.profiles.active=test",
                "spring.datasource.url=jdbc:h2:mem:hive-context;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
class UniqueRuntimeComponentTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void springBeanNamesAreUniqueInTheAuthoritativeApplicationContext() {
        assertThat(context.getBeanDefinitionNames()).doesNotHaveDuplicates();
    }

    @Test
    void resolvedRequestMappingsExposeConcreteManagementRoutes() {
        assertThat(context.getEnvironment().getProperty("server.servlet.context-path")).isEqualTo("/api");
        assertResolvedMapping(AuthController.class, HttpMethod.POST, "/auth/login");
        assertResolvedMapping(OrderController.class, HttpMethod.GET, "/order/page");
        assertResolvedMapping(OrderController.class, HttpMethod.POST, "/order/create");
    }

    @Test
    void criticalRuntimeComponentsExistExactlyOnce() {
        assertThat(context.getBeansOfType(WebMvcConfigurer.class).values())
                .filteredOn(bean -> AopUtils.getTargetClass(bean).getPackageName().startsWith("my."))
                .hasSize(1);
    }

    private void assertResolvedMapping(Class<?> controller, HttpMethod httpMethod, String path) {
        assertThat(handlerMapping.getHandlerMethods().entrySet())
                .filteredOn(entry -> entry.getValue().getBeanType().equals(controller))
                .filteredOn(entry -> entry.getKey().getPatternValues().contains(path))
                .filteredOn(entry -> entry.getKey().getMethodsCondition().getMethods().stream()
                        .anyMatch(method -> method.name().equals(httpMethod.name())))
                .as(httpMethod + " " + path + " (servlet context /api is applied separately)")
                .hasSize(1);
    }
}
