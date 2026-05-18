package my.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialHardeningStaticTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java");
    private static final Pattern SELECT_STAR = Pattern.compile("(?is)SELECT\\s+\\*\\s+FROM");
    private static final List<String> FORBIDDEN_TEXT = List.of("@Scheduled", "companyAttendanceRule");

    @Test
    void sourceShouldNotReintroduceLegacySchedulerOrCacheKey() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_TEXT) {
                if (content.contains(forbidden)) {
                    violations.add(file + " contains " + forbidden);
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void mybatisInlineSqlShouldAvoidSelectStar() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (SELECT_STAR.matcher(content).find()) {
                violations.add(file.toString());
            }
        }
        assertTrue(violations.isEmpty(), "Use explicit columns instead of SELECT *:\n" + String.join(System.lineSeparator(), violations));
    }

    @Test
    void criticalWriteEndpointsShouldHaveOperationAudit() throws IOException {
        Map<String, List<String>> criticalMappings = Map.ofEntries(
                Map.entry("my/management/controller/AuthController.java", List.of("/login", "/password-reset", "/initial-password", "/scan-login/confirm")),
                Map.entry("my/management/controller/ApprovalController.java", List.of("/leave/audit", "/finance/audit", "/finance/submit", "/resignation/submit", "/resignation/audit")),
                Map.entry("my/management/controller/EmployeeController.java", List.of("/create", "/update", "/change-status", "/batch-update", "/import")),
                Map.entry("my/management/controller/OrderController.java", List.of("/sales/create", "/sales/save/{orderId}", "/sales/update/{orderId}", "/production/create", "/production/save/{orderId}", "/production/update/{orderId}")),
                Map.entry("my/management/controller/RoleController.java", List.of("/create", "/role/update")),
                Map.entry("my/management/controller/ReceiptPrintController.java", List.of("/print/update", "/print/mark-printed", "/print/cancel", "/template/save", "/template/{id}/default"))
        );
        assertCriticalMappingsAudited(criticalMappings);
    }

    private static void assertCriticalMappingsAudited(Map<String, List<String>> criticalMappings) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : criticalMappings.entrySet()) {
            Path file = MAIN_SOURCE.resolve(entry.getKey());
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String mapping : entry.getValue()) {
                int mappingIndex = content.indexOf("@PostMapping(\"" + mapping + "\")");
                if (mappingIndex < 0) {
                    mappingIndex = content.indexOf("@DeleteMapping(\"" + mapping + "\")");
                }
                if (mappingIndex < 0) {
                    violations.add(file + " missing mapping " + mapping);
                    continue;
                }
                int methodIndex = content.indexOf("public ", mappingIndex);
                String annotationBlock = content.substring(mappingIndex, methodIndex < 0 ? Math.min(content.length(), mappingIndex + 500) : methodIndex);
                if (!annotationBlock.contains("@CollectLog(")) {
                    violations.add(file + " mapping " + mapping + " missing @CollectLog");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Critical write endpoints must be audited:\n" + String.join(System.lineSeparator(), violations));
    }

    private static List<Path> javaFiles() throws IOException {
        if (!Files.exists(MAIN_SOURCE)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(MAIN_SOURCE)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}
