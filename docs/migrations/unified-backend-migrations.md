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

### Order multi-shipment clean-launch gate

`V20260717_001_order_multi_shipment.sql` is a clean-launch destructive contract and is supported only for the formal clean launch. Before deploying this release, back up if required and clear all old business data; the production business database must be empty before initialization. Do not retain pre-launch order rows.

The release does not backfill `sales_order.express_company` or `sales_order.express_no`, does not preserve those columns, and does not provide compatibility reads. `hive_schema_baseline_v2.sql` is immutable and still contains the legacy columns; it does not contain `sales_order_shipment`. `import-baseline-to-shadow.sh` registers history only through `V20260716_001`, then the versioned runner executes `V20260717_001` to create the shipment table and drop the retired columns.

The routine entry also calls `check-order-multi-shipment-clean-launch.sh` before the versioned runner. If `V20260717_001` is pending, the helper fails closed unless `sales_order` exists and `SELECT COUNT(*)` returns exactly `0`. Existing rows require the formal cleanup process; query errors, malformed counts, missing tables and non-success history records are all blocking states. A previously executed `SUCCESS` returns before the row query because the destructive SQL is no longer eligible to execute. The gate has no bypass environment variable.

## Existing database

The general routine below applies only to releases whose migrations support retained data. It must not be used to carry legacy business data across the `V20260717_001` clean-launch boundary.

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
