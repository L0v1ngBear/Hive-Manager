package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedBackendSourceGuardTest {

    @Test
    void unifiedSourceMustNotContainLegacyRuntimeRoots() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            String sourcePaths = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.joining("\n"));

            assertThat(sourcePaths)
                    .doesNotContain("my/management", "my/hive_back");
        }
    }

    @Test
    void publicConfigurationMustUseOnlyApiContext() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(yaml)
                .contains("context-path: /api")
                .doesNotContain("context-path: /web");
    }
}
