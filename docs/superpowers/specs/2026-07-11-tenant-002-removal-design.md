# TENANT_002 Removal Design

Date: 2026-07-11

## Objective

Permanently remove the retired `TENANT_002` test tenant and all of its tenant-owned database rows, Redis cache entries, and uploaded files before commercial launch. The cleanup must not affect `TENANT_001`, the internal `super` tenant, global permissions, migration history, or any other tenant.

## Release Ordering

The historical migration `V20260530_001_second_tenant_seed.sql` remains byte-for-byte unchanged. It may create `TENANT_002` during migration, so cleanup runs only after the normal release migration and functional acceptance:

1. Run versioned migrations and release acceptance.
2. Run cleanup in preview mode and review counts.
3. Create and verify a fresh database backup.
4. Re-run with explicit destructive confirmation.
5. Verify zero database, cache, and upload residue.

The cleanup is a manual commercial-launch operation and is never added to `migration_manifest.txt` or the normal restart path.

## Artifacts

- `db-migrations/manual/V20260711_001_remove_tenant_002.sql`: fixed-target database cleanup executed in one transaction.
- `scripts/manual-remove-tenant-002.sh`: preview, backup, confirmation, execution, cache/file cleanup, and verification wrapper.
- `management-ui/tests/deploy-tenant-002-cleanup.test.js`: static safety contract for the deploy package.
- `UPLOAD_STEPS.md`: documents the required post-migration cleanup command and verification output.

## Safety Model

- The target is hard-coded as `TENANT_002`; callers cannot supply another tenant code.
- Running without `CONFIRM_REMOVE_TENANT_002=YES` is read-only and prints candidate counts.
- Destructive mode requires a fresh verified MySQL backup before any delete.
- The SQL explicitly deletes role-permission rows whose owning roles belong to `TENANT_002`, then deletes every table in the `hive` schema that has a `tenant_code` column, and deletes the tenant row last.
- Dynamically discovered table names must come only from `information_schema` and match `^[A-Za-z0-9_]+$` before being quoted.
- All database deletes execute in one transaction with foreign-key checks restored on both success and failure.
- The script refuses to target `TENANT_001`, `super`, an empty tenant code, or a database other than the configured Hive business database.
- Upload deletion is limited to resolved directories named exactly `TENANT_002` beneath `/root/hive/uploads`.
- Redis cleanup removes only keys whose names contain the exact `TENANT_002` marker.

## Data Coverage

The cleanup includes tenant-owned users, employee extensions and change logs, departments, positions, roles, role bindings, user overrides, attendance settings and records, customers and projects, orders and status logs, approvals and auditor records, inventory and outbound records, quality and after-sales records, equipment inspections, announcements and read receipts, installation tasks, print tasks, documents, notifications, operation logs, subscription bindings, settings, and any future table carrying `tenant_code`.

Global `sys_permission` rows, schema migration history, shared scheduler schema, `TENANT_001`, and the internal `super` tenant remain untouched.

## Verification

Automated checks must prove:

- preview mode performs no delete;
- destructive mode is gated by the exact confirmation variable;
- backup and backup verification run before the SQL file;
- the historical seed migration is not modified;
- neither `TENANT_001` nor `super` appears in a delete predicate;
- the SQL deletes `tenant` last and commits atomically;
- post-cleanup database residue across all `tenant_code` tables is zero;
- Redis and upload cleanup are narrowly scoped to `TENANT_002`;
- the operation is idempotent when `TENANT_002` is already absent.

## Failure Handling

Any backup, SQL, database verification, cache cleanup, or path-boundary check failure exits non-zero. Database failure rolls back the transaction. File cleanup starts only after database verification succeeds. The latest backup remains available for manual restoration; the script does not attempt an automatic data restore.
