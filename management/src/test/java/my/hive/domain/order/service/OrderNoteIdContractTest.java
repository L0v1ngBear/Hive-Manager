package my.hive.domain.order.service;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.domain.order.model.dto.SalesOrderNoteSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderNote;
import my.hive.domain.order.model.vo.SalesOrderNoteVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNoteIdContractTest {

    private static final long EXISTING_SNOWFLAKE_ID = 2016723456789012345L;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesExistingLongIdAsExactJsonString() throws Exception {
        SalesOrderNoteVO note = new SalesOrderNoteVO();
        note.setId(EXISTING_SNOWFLAKE_ID);

        JsonNode payload = objectMapper.readTree(objectMapper.writeValueAsBytes(note));

        assertTrue(payload.get("id").isTextual());
        assertEquals(Long.toString(EXISTING_SNOWFLAKE_ID), payload.get("id").textValue());
    }

    @Test
    void acceptsStringIdFromBrowserWithoutPrecisionLoss() throws Exception {
        SalesOrderNoteSaveRequest request = objectMapper.readValue(
                """
                {"id":"2016723456789012345","content":"保留旧备注","version":2}
                """,
                SalesOrderNoteSaveRequest.class);

        assertEquals(EXISTING_SNOWFLAKE_ID, request.getId());
    }

    @Test
    void generatesNewNoteIdsWithDatabaseAutoIncrement() throws Exception {
        TableId tableId = SalesOrderNote.class.getDeclaredField("id").getAnnotation(TableId.class);

        assertEquals(IdType.AUTO, tableId.type());
    }

    @Test
    void migrationPreservesExistingIdsAndEnablesAutoIncrement() throws Exception {
        Path migration = Path.of("..", "db-migrations", "migrations",
                "V20260730_001_sales_order_note_auto_increment.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        String manifest = Files.readString(
                Path.of("..", "db-migrations", "migration_manifest.txt"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("ALTER TABLE `sales_order_note`"));
        assertTrue(sql.contains("`id` bigint NOT NULL AUTO_INCREMENT"));
        assertTrue(manifest.contains("migrations/V20260730_001_sales_order_note_auto_increment.sql"));
    }
}
