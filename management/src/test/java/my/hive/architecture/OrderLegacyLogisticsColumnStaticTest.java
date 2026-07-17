package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderLegacyLogisticsColumnStaticTest {

    private static final Path ORDER_MAPPER_SOURCE = Path.of(
            "src", "main", "java", "my", "hive", "domain", "order", "mapper");
    private static final Pattern LEGACY_LOGISTICS_COLUMN = Pattern.compile(
            "\\bexpress_(?:company|no)\\b", Pattern.CASE_INSENSITIVE);

    @Test
    void activeOrderMappersMustNotReferenceRetiredScalarLogisticsColumns() throws IOException {
        List<Path> mapperFiles;
        try (Stream<Path> files = Files.list(ORDER_MAPPER_SOURCE)) {
            mapperFiles = files.filter(path -> path.getFileName().toString().endsWith("Mapper.java")).toList();
        }
        assertFalse(mapperFiles.isEmpty(), "No active order mapper sources were found");

        for (Path mapperFile : mapperFiles) {
            String source = Files.readString(mapperFile, StandardCharsets.UTF_8);
            assertFalse(LEGACY_LOGISTICS_COLUMN.matcher(source).find(),
                    "Retired scalar logistics column remains in " + mapperFile);
        }
    }
}
