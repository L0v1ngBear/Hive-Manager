package my.hive.domain.approval.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderApprovalOrMigrationContractTest {

    private static final String MIGRATION = "migrations/V20260903_001_order_approval_or_reconciliation.sql";

    @Test
    void migrationChangesOnlyOrderApprovalRowsAndIsRegistered() throws Exception {
        Path migrationRoot = Path.of("..", "db-migrations");
        byte[] migrationBytes = Files.readAllBytes(migrationRoot.resolve(MIGRATION));
        String sql = new String(migrationBytes, StandardCharsets.UTF_8);
        String manifest = Files.readString(migrationRoot.resolve("migration_manifest.txt"));
        String checksums = Files.readString(migrationRoot.resolve("migration_checksums.sha256"));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(migrationBytes));

        assertEquals(2, sql.split("WHERE approval_type = 'ORDER'", -1).length - 1);
        assertTrue(sql.contains("UPDATE approval_default_auditor"));
        assertTrue(sql.contains("UPDATE approval_auditor_candidate"));
        assertTrue(manifest.lines().anyMatch(MIGRATION::equals));
        assertTrue(checksums.lines().anyMatch(line -> line.equals(digest + "  " + MIGRATION)));
    }
}
