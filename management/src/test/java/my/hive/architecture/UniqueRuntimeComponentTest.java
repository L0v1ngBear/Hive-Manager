package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Compile-safe source gate for Task 1. Task 2 upgrades this to inspect the unified
 * Spring ApplicationContext once {@code my.hive.HiveApplication} exists.
 */
class UniqueRuntimeComponentTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Pattern COMPONENT_CLASS = Pattern.compile(
            "@(Component|Service|Repository|Controller|RestController|Configuration)(?:\\s*\\(\\s*\\\"([^\\\"]+)\\\"\\s*\\))?"
                    + "[\\s\\S]*?\\bclass\\s+(\\w+)");
    private static final Pattern BEAN_METHOD = Pattern.compile(
            "@Bean(?:\\s*\\(\\s*(?:name|value)\\s*=\\s*\\\"([^\\\"]+)\\\"\\s*\\))?"
                    + "\\s*(?:public|protected|private)?\\s*[\\w<>?,.\\[\\] ]+\\s+(\\w+)\\s*\\(");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern CLASS_MAPPING = Pattern.compile(
            "@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\\\"([^\\\"]*)\\\"\\s*\\)[\\s\\S]*?\\bclass\\s+\\w+");
    private static final Pattern METHOD_MAPPING = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch)Mapping\\s*(?:\\(\\s*(?:value\\s*=\\s*)?\\\"([^\\\"]*)\\\"[^)]*\\))?");

    @Test
    void springBeanNamesMustBeUnique() throws IOException {
        Map<String, List<Path>> declarations = new HashMap<>();
        for (SourceFile source : javaSources()) {
            Matcher components = COMPONENT_CLASS.matcher(source.content());
            while (components.find()) {
                String beanName = components.group(2) == null
                        ? Introspector.decapitalize(components.group(3))
                        : components.group(2);
                declarations.computeIfAbsent(beanName, ignored -> new ArrayList<>()).add(source.path());
            }
            Matcher beans = BEAN_METHOD.matcher(source.content());
            while (beans.find()) {
                String beanName = beans.group(1) == null ? beans.group(2) : beans.group(1);
                declarations.computeIfAbsent(beanName, ignored -> new ArrayList<>()).add(source.path());
            }
        }

        assertThat(duplicates(declarations)).as("duplicate Spring bean names").isEmpty();
    }

    @Test
    void requestMappingsMustBeUniqueByHttpMethodAndPath() throws IOException {
        Map<String, List<Path>> declarations = new HashMap<>();
        for (SourceFile source : javaSources()) {
            String classPath = firstGroup(CLASS_MAPPING, source.content());
            Matcher mappings = METHOD_MAPPING.matcher(source.content());
            while (mappings.find()) {
                String method = mappings.group(1).toUpperCase();
                String methodPath = mappings.group(2) == null ? "" : mappings.group(2);
                String key = method + " " + normalizePath(classPath, methodPath);
                declarations.computeIfAbsent(key, ignored -> new ArrayList<>()).add(source.path());
            }
        }

        assertThat(duplicates(declarations)).as("duplicate request mappings").isEmpty();
    }

    @Test
    void criticalRuntimeComponentsMustExistExactlyOnce() throws IOException {
        List<SourceFile> sources = javaSources();

        assertSoftly(softly -> {
            softly.assertThat(countClass(sources, "PrintTaskController"))
                    .as("PrintTaskController cardinality").isOne();
            softly.assertThat(countContaining(sources, "implements WebMvcConfigurer"))
                    .as("WebMvcConfigurer cardinality").isOne();
            softly.assertThat(countClass(sources, "XxlJobConfig"))
                    .as("XXL-JOB executor configuration cardinality").isOne();
            softly.assertThat(countContaining(sources, "OperationLog", "@RabbitListener"))
                    .as("operation-log listener cardinality").isOne();
        });
    }

    private static List<SourceFile> javaSources() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> new SourceFile(path, read(path)))
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private static Map<String, List<Path>> duplicates(Map<String, List<Path>> declarations) {
        Map<String, List<Path>> duplicates = new HashMap<>();
        declarations.forEach((key, paths) -> {
            if (paths.size() > 1) {
                duplicates.put(key, paths);
            }
        });
        return duplicates;
    }

    private static String firstGroup(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String normalizePath(String classPath, String methodPath) {
        String combined = ("/" + classPath + "/" + methodPath).replaceAll("/+", "/");
        return combined.length() > 1 && combined.endsWith("/")
                ? combined.substring(0, combined.length() - 1)
                : combined;
    }

    private static long countClass(List<SourceFile> sources, String className) {
        return sources.stream().filter(source -> {
            Matcher matcher = CLASS_DECLARATION.matcher(source.content());
            return matcher.find() && matcher.group(1).equals(className);
        }).count();
    }

    private static long countContaining(List<SourceFile> sources, String... fragments) {
        return sources.stream()
                .filter(source -> Stream.of(fragments).allMatch(source.content()::contains))
                .count();
    }

    private record SourceFile(Path path, String content) {
    }
}
