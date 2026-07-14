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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    void resolvedRequestMappingsAreUniqueBySpringRequestCondition() {
        Map<RequestMappingInfo, List<HandlerMethod>> registrations = new LinkedHashMap<>();
        handlerMapping.getHandlerMethods().forEach((mapping, method) ->
                registrations.compute(mapping, (ignored, methods) -> methods == null
                        ? List.of(method)
                        : append(methods, method)));

        assertThat(registrations).allSatisfy((mapping, methods) ->
                assertThat(methods).as(mapping.toString()).hasSize(1));

        assertThat(handlerMapping.getHandlerMethods().values())
                .extracting(HandlerMethod::getBeanType)
                .contains(AuthController.class, OrderController.class);
    }

    @Test
    void criticalRuntimeComponentsExistExactlyOnce() {
        assertThat(context.getBeansOfType(WebMvcConfigurer.class).values())
                .filteredOn(bean -> AopUtils.getTargetClass(bean).getPackageName().startsWith("my."))
                .hasSize(1);
    }

    private static List<HandlerMethod> append(List<HandlerMethod> methods, HandlerMethod method) {
        return java.util.stream.Stream.concat(methods.stream(), java.util.stream.Stream.of(method)).toList();
    }
}
