package my.management;

import my.hive.common.autoconfigure.HiveCommonAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "my.management")
@MapperScan("my.management.module.**.mapper")
@Import(HiveCommonAutoConfiguration.class)
@EnableScheduling
public class ManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementApplication.class, args);
    }

}
