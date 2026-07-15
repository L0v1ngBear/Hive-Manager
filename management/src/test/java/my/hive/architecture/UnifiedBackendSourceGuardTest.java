package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedBackendSourceGuardTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void unifiedSourceMustNotContainLegacyRuntimeRoots() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            String sourcePaths = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.joining("\n"));

            assertThat(sourcePaths)
                    .doesNotContain("my/management", "my/hive_back");
        }
    }

    @Test
    void unifiedRuntimeMustHaveOneApplicationAndNoLegacyImports() throws IOException {
        List<Path> javaFiles;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        String allSources = javaFiles.stream()
                .map(UnifiedBackendSourceGuardTest::readUnchecked)
                .collect(Collectors.joining("\n"));

        assertThat(count(allSources, "@SpringBootApplication")).isEqualTo(1);
        assertThat(allSources)
                .doesNotContain("import my.management", "import my.hive_back",
                        "package my.management", "package my.hive_back");
        assertThat(Pattern.compile("[\\\"']/web(?:/|[\\\"'])").matcher(allSources).find()).isFalse();
    }

    @Test
    void permissionCatalogV3MustContainOnlyExactCodes() throws IOException {
        String catalog = Files.readString(
                MAIN_JAVA.resolve("my/hive/shared/permission/PermissionCatalogV3.java"));
        Matcher constants = Pattern.compile(
                "public static final String\\s+\\w+\\s*=\\s*\\\"([^\\\"]*)\\\";")
                .matcher(catalog);

        int count = 0;
        while (constants.find()) {
            count++;
            assertThat(constants.group(1)).doesNotContain("*");
        }
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void publicConfigurationMustUseOnlyApiContext() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(yaml)
                .contains("context-path: /api")
                .doesNotContain("context-path: /web");
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private static int count(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
