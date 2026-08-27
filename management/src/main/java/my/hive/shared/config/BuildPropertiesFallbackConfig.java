package my.hive.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * IDEA 直接运行主类时不会执行 Maven 的 build-info 目标，因此不会自动创建 BuildProperties。
 * 正式构建产物包含 build-info 时，此配置不会生效。
 */
@Configuration
public class BuildPropertiesFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(BuildProperties.class)
    public BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "hive-backend");
        properties.setProperty("version", "dev");
        return new BuildProperties(properties);
    }
}
