package my.hive.architecture;

import my.hive.shared.permission.PermissionCatalogV3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SinglePermissionCatalogTest {

    @Test
    void sharedCatalogIsTheOnlyProductionCatalogType() throws Exception {
        Path source = Path.of("src/main/java");
        try (var paths = Files.walk(source)) {
            assertThat(paths.filter(path -> path.getFileName().toString().equals("PermissionCatalogV3.java")))
                    .containsExactly(source.resolve("my/hive/shared/permission/PermissionCatalogV3.java"));
        }
        assertThat(new PermissionCatalogV3().codes()).contains("order:list");
    }
}
