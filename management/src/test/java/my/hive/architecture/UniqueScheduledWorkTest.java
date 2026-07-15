package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueScheduledWorkTest {

    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final Pattern XXL_JOB = Pattern.compile("@XxlJob\\(\\\"([^\\\"]+)\\\"\\)");
    private static final Pattern RABBIT_LISTENER = Pattern.compile("@RabbitListener\\(queues\\s*=\\s*\\\"([^\\\"]+)\\\"\\)");

    @Test
    void requiredXxlHandlersExistExactlyOnce() throws IOException {
        List<String> handlers = annotationValues(XXL_JOB);

        assertThat(handlers).containsExactlyInAnyOrder(
                "attendanceDailyStatJob",
                "inventoryDailyStatJob",
                "notificationClosedLoopJob",
                "runtimeStabilityAuditJob",
                "dbCapacityReportJob",
                "dbCleanupJob"
        );
        assertThat(duplicates(handlers)).isEmpty();
    }

    @Test
    void rabbitQueuesHaveOneConsumerDeclarationEach() throws IOException {
        List<String> queues = annotationValues(RABBIT_LISTENER);

        assertThat(queues).contains("${hive.operation-log.rabbit-queue:hive.operation.log.queue}");
        assertThat(duplicates(queues)).isEmpty();
    }

    @Test
    void schedulerInfrastructureUsesOnlyCanonicalPackage() throws IOException {
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/scheduler/XxlJobConfig.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/scheduler/XxlJobProperties.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/scheduler/DatabaseMaintenanceJobHandler.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/scheduler/StatisticsJobHandler.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/management/config/XxlJobConfig.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/config/XxlJobProperties.java")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/module/maintenance")).doesNotExist();
    }

    private List<String> annotationValues(Pattern pattern) throws IOException {
        try (var paths = Files.walk(MAIN_SOURCE)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> matchValues(path, pattern).stream())
                    .toList();
        }
    }

    private List<String> matchValues(Path path, Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(Files.readString(path));
            java.util.ArrayList<String> values = new java.util.ArrayList<>();
            while (matcher.find()) {
                values.add(matcher.group(1));
            }
            return values;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private Map<String, Long> duplicates(List<String> values) {
        return values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
