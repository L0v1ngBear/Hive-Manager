package my.management.security;

import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.permission.PermissionCatalogV3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimePermissionCatalogV3ContractTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");
    private static final Pattern PERMISSION_LITERAL = Pattern.compile(
            "\\\"([a-z][a-z0-9-]*(?::[a-z0-9-]+)+)\\\"");

    private final PermissionCatalogV3 catalog = new PermissionCatalogV3();

    @Test
    void everyRuntimePermissionConstantIsAnAssignableV3Leaf() throws IllegalAccessException {
        Set<String> invalid = new LinkedHashSet<>();
        for (Field field : PermissionCatalogV3.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || field.getType() != String.class
                    || !field.getName().startsWith("CODE_")
                    || field.getName().endsWith("_PREFIX")) {
                continue;
            }
            String code = (String) field.get(null);
            if (!catalog.isAssignable(code)) {
                invalid.add(field.getName() + "=" + code);
            }
        }
        assertEquals(Set.of(), invalid, "Runtime constants must reference assignable V3 leaves");
    }

    @Test
    void runtimeSourcesDoNotEmbedPermissionsOutsideTheV3Catalog() throws Exception {
        Set<String> invalid = new LinkedHashSet<>();
        try (var paths = Files.walk(MAIN_JAVA)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(item -> item.toString().endsWith(".java"))
                    .filter(item -> !item.endsWith("PermissionCatalogV3.java"))
                    .filter(item -> !item.endsWith("PermissionCatalogV3.java"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                Matcher matcher = PERMISSION_LITERAL.matcher(source);
                while (matcher.find()) {
                    String code = matcher.group(1);
                    if (!catalog.isAssignable(code)) {
                        invalid.add(MAIN_JAVA.relativize(path) + " -> " + code);
                    }
                }
            }
        }
        assertTrue(invalid.isEmpty(), "Runtime source contains permissions outside V3: " + invalid);
    }
}
