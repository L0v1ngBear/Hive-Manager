package my.hive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single executable entry point for the converged Hive backend.
 */
@SpringBootApplication
@MapperScan({"my.hive.domain.**.mapper", "my.hive.infrastructure.**.mapper"})
public class HiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiveApplication.class, args);
    }
}
