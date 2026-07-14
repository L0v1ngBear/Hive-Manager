package my.hive.shared;

import my.hive.HiveApplication;
import my.hive.shared.auth.AuthenticatedSessionService;
import my.hive.shared.auth.TokenService;
import my.hive.shared.security.PermissionEvaluator;
import my.hive.shared.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
class SharedInfrastructureContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void exposesExactlyOneSharedRuntimeStack() {
        assertThat(context.getBeansOfType(TokenService.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthenticatedSessionService.class)).hasSize(1);
        assertThat(context.getBeansOfType(TenantContext.class)).hasSize(1);
        assertThat(context.getBeansOfType(PermissionEvaluator.class)).hasSize(1);
        assertThat(context.getBeansOfType(WebMvcConfigurer.class).values())
                .filteredOn(bean -> AopUtils.getTargetClass(bean).getPackageName().startsWith("my."))
                .hasSize(1);
    }
}
