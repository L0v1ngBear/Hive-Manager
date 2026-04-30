package my.management;

import my.hive.common.autoconfigure.HiveCommonAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "my.management")
@MapperScan("my.management.module.**.mapper")
@Import(HiveCommonAutoConfiguration.class)
public class ManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementApplication.class, args);
    }

}
