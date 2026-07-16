# Unified backend migrations

## Authoritative contract

`db-migrations/migration_manifest.txt` is the only ordered historical migration list. Every listed `V*.sql` file is immutable after release and is protected by `db-migrations/migration_checksums.sha256`. A schema change always uses a new migration version.

The repository exposes one routine migration command:

```bash
bash scripts/migrate-db.sh
```

The command accepts only a managed database with a non-empty successful migration history and no failed records. It does not initialize an empty database, repair checksum drift or execute SQL from application resources.

## Fresh database

`db-migrations/baseline/hive_schema_baseline_v2.sql` is the current schema-only baseline. `db-migrations/seeds/system_permission_catalog_v3.sql` is the current permission catalog seed. Both files are checksum-protected release assets and contain no business data or retired AI tables.

Initialize a fresh empty database explicitly:

```bash
CONFIRM_FRESH_DATABASE_INITIALIZATION=YES \
  HIVE_BOOTSTRAP_OWNER_PASSWORD='replace-with-a-strong-password' \
  bash db-migrations/scripts/initialize-fresh-database.sh
```

The initializer imports the v2 baseline and permission seed, creates only `TENANT_001` and one owner account, and marks the bootstrap password for mandatory first-login replacement.

## Existing database

Before migration:

1. Stop backend writes.
2. Create and verify a full database backup.
3. Verify migration checksums and database state.
4. Execute the manifest in order.
5. Verify required tables, columns and failed migration count.

Routine release scripts do not recreate MySQL or Redis. A failed migration leaves the backend stopped so old application code cannot continue writing against a partially migrated schema.

## Restore

Use `db-migrations/scripts/restore-verified-backup.sh`. It imports the selected backup into a shadow database and verifies core Hive tables before an explicitly confirmed online replacement.

Never:

- modify a migration that was already executed;
- substitute a historical migration with a file from another release;
- mark migrations successful without executing them;
- run partial tenant/business reset scripts during release;
- use a file-only rollback after a forward database migration;
- write reverse SQL as an emergency shortcut.

## Retired data

Formal launch uses a clean database, so retired AI advice, behavior-event and second-tenant bootstrap data are not carried forward. Historical migration files remain only as immutable evidence for databases that have already recorded those versions.
