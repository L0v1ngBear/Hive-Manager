# Hive Release, Database, and Order Audit Repair Design

## Goal

Eliminate the release blockers found in the 2026-07-16 full audit without changing any historical `V*.sql` migration. A routine release must preserve data services, an incomplete database must stop the release before the backend starts, a fresh installation must use an explicit verified path, and order operation records must be queryable from order details.

## Release Boundaries

- `mysql`, `redis`, and other persistent services are server-owned runtime state. A normal application release must not force-recreate them.
- Upload staging contains only versioned configuration, scripts, one backend JAR, the clean management UI tree, migrations, and metadata. It never contains `.env`, certificates, persistent data, uploads, logs, backups, reports, snapshots, or the mini-program package.
- Server synchronization uses an allowlist and deletes stale files only inside replaceable application paths. Runtime-owned paths are always preserved.
- A failed migration leaves the backend stopped. Automatic recovery may restore release files, but it must not start an unverified backend or claim to restore an overwritten image.

## Database Paths

### Existing database upgrade

`scripts/migrate-db.sh` remains the only online migration entry. Before running versioned migrations it verifies that core baseline tables (`tenant`, `user`, `sys_permission`, `sys_role`) exist. If they do not, it fails with an explicit fresh-initialization or backup-restore instruction.

### Fresh installation

Fresh initialization is explicit and destructive only with `CONFIRM_FRESH_DATABASE_INITIALIZATION=YES`. It is allowed only when the target database has no business tables, or when a separate reset confirmation has been supplied after a verified backup. It imports a current schema-only baseline, registers its own checksummed baseline marker and all represented historical versions, runs versions newer than the baseline, provisions the single `TENANT_001` owner from server-owned environment values, and runs the normal schema verifier.

The old baseline remains unchanged. A new baseline file and marker are introduced so previously registered baseline checksums are not silently rewritten.

### Backup and restore

Backup verification must inspect required schema markers (`tenant`, `user`, `sys_permission`, `schema_migration_history`) in the dump, not only its gzip header. A guarded restore script restores into a temporary database first, verifies it, and only then permits an explicit online replacement.

## Order Operation Records

`operation_log` remains restricted to `module=order`. All order mutations, including status-log time correction and order approval, create records. A tenant-isolated read endpoint returns only order records for the requested order number and requires `order:detail`.

Management and mini-program order details show two independent sections: status transitions and operation records. Operation records expose action, description, operator, success/failure, time, and safe error text; request payloads and sensitive fields are not returned.

## Configuration and Documentation

- Runtime validation rejects `TENANT_002`, a tenant count other than `1`, and missing required single-tenant keys.
- The current certificate is server-owned; checks continue to require more than 30 days of validity.
- Release documentation and metadata counts are generated from artifacts rather than hand-maintained examples.
- Obsolete server scripts such as `accept-low-cost-release.sh` are removed by the allowlisted synchronization process, not by broad deletion of the runtime directory.

## Verification

- Database script contract tests cover empty, incomplete, and existing database decisions.
- A local MySQL shadow import proves the new baseline and post-baseline migrations produce the required schema.
- Backend tests cover tenant isolation and permission enforcement for operation-log reads.
- Management tests cover operation-record rendering and permission behavior.
- Existing 224 backend tests, 257 management tests, production UI build, release-integrity checks, and package file/hash comparisons must remain green.
