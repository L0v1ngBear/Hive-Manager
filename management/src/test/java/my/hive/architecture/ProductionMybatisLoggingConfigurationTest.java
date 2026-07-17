package my.hive.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.Environment;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionMybatisLoggingConfigurationTest {

    private static final String LOG_IMPLEMENTATION_PROPERTY = "mybatis-plus.configuration.log-impl";
    private static final String STDOUT_IMPLEMENTATION = "org.apache.ibatis.logging.stdout.StdOutImpl";
    private static final String NO_LOGGING_IMPLEMENTATION = "org.apache.ibatis.logging.nologging.NoLoggingImpl";

    @Test
    void prodProfileMustOverrideCommonMybatisPlusStdoutLogging() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run()) {
            Environment environment = context.getEnvironment();

            assertEquals(STDOUT_IMPLEMENTATION, commonLogImplementation(),
                    "The common application configuration must retain development SQL logging");
            assertEquals(NO_LOGGING_IMPLEMENTATION, environment.getProperty(LOG_IMPLEMENTATION_PROPERTY),
                    "The effective prod MyBatis-Plus configuration must disable SQL and parameter logging");
        }
    }

    private String commonLogImplementation() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yaml.getObject();
        return properties.getProperty(LOG_IMPLEMENTATION_PROPERTY);
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
    }
}
