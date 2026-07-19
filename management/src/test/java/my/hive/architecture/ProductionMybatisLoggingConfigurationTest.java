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
    private static final String NO_LOGGING_IMPLEMENTATION = "org.apache.ibatis.logging.nologging.NoLoggingImpl";

    @Test
    void defaultDevAndProdProfilesNeverEmitSqlParameterOrResultValues() {
        assertEquals(NO_LOGGING_IMPLEMENTATION, yamlProperties("application.yaml")
                        .getProperty(LOG_IMPLEMENTATION_PROPERTY),
                "The common MyBatis configuration must never print SQL parameters or result rows");
        assertEquals("WARN", yamlProperties("application-dev.yaml")
                        .getProperty("logging.level.my.hive.domain.auth.mapper"),
                "The dev auth mapper logger must not expose parameter values at TRACE");
        assertEquals("WARN", yamlProperties("application-dev.yaml")
                        .getProperty("logging.level.org.apache.ibatis"),
                "The dev MyBatis logger must not expose parameter values at TRACE");

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run()) {
            Environment environment = context.getEnvironment();

            assertEquals(NO_LOGGING_IMPLEMENTATION, environment.getProperty(LOG_IMPLEMENTATION_PROPERTY),
                    "The effective prod MyBatis-Plus configuration must disable SQL and parameter logging");
        }
    }

    private Properties yamlProperties(String resource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(resource));
        return yaml.getObject();
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
    }
}
