package my.hive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single executable entry point for the converged Hive backend.
 *
 * <p>The transitional {@code my} scan root deliberately includes the existing
 * management packages until each domain is moved below {@code my.hive}. This
 * keeps current endpoints available while retaining one authoritative context.</p>
 */
@SpringBootApplication(scanBasePackages = "my")
@MapperScan({"my.management.module.**.mapper", "my.hive.domain.**.mapper"})
public class HiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiveApplication.class, args);
    }
}
