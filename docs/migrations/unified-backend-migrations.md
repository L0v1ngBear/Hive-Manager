# Unified Backend Migrations

## Contract

Historical versioned SQL is immutable. Any convergence schema change must be a new `V*.sql` migration and must update the migration manifest and checksum snapshot. Production cutover does not preserve retired business data, tokens, caches, or API compatibility behavior. The single repository migration command is `bash scripts/migrate-db.sh`.

## Module status

| Module | Status |
| --- | --- |
| foundation | COMPLETE |
| permission | COMPLETE |
| auth | COMPLETE |

Task 4 requires no schema change. Both clients use the existing user `auth_version` for versioned sessions and logout invalidation.
| order | COMPLETE |
| approval | COMPLETE |
| inventory | COMPLETE |
| quality | COMPLETE |
| installation | COMPLETE |
| customer | COMPLETE |
| document | COMPLETE |
| equipment | COMPLETE |
| label | COMPLETE |
| print | COMPLETE |
| notification | COMPLETE |
| attendance | COMPLETE |
| migration | COMPLETE |
| deployment | PLANNED |

## Task 6 migration note

Inventory, quality, and installation convergence is a Java package and API adapter consolidation. It does not require modifying historical SQL or adding a new versioned migration; the existing `cloth`, `inventory_record`, `inventory_setting`, `bad_product_record`, and `installation_task` table contracts remain canonical.

## Task 7 migration note

Customer, document, equipment, label-template, receipt-print, and print-task convergence is a Java package/API consolidation. It does not modify historical SQL and does not require a new migration; the existing customer, document, equipment, label template, outbound receipt, and `print_task` table contracts remain canonical.

## Task 8 migration note

Task 8 does not modify an executed historical SQL file. The unified implementation consumes the existing `notification_record`, `enterprise_announcement`, `attendance_record`, `tenant_attendance_rule`, `tenant_attendance_location`, `employee_attendance_location`, `attendance_statics`, `inventory_statics`, and `wechat_subscribe_user` contracts already present in the approved deployment baseline/version history. Task 10 will import that history unchanged into the repository and verify its manifest checksums; any subsequently discovered schema delta must be introduced as a new version file.

## Task 9 migration note

Removing the Java legacy roots and narrowing component/mapper scanning does not change the database schema. Task 9 therefore adds no migration and modifies no historical SQL. The canonical employee, role, permission, order, approval, tenant, notification, attendance, print, and other domain mappers continue to use the existing tables; migration-history import and checksum enforcement remain Task 10 work.

## Task 10 authoritative migration source

`db-migrations/migration_manifest.txt` is the only ordered version list and currently matches all 74 files under `db-migrations/migrations`. `db-migrations/migration_checksums.sha256` records the SHA-256 of every manifest file. The Node gate also pins known executed migrations including the second-tenant seed, installation schema, Permission Catalog V3, and permission-relation convergence versions.

The migration runner refuses a previously successful version whose checksum has changed. The schema-only baseline passed `db-migrations/scripts/verify-schema-only-baseline.sh`. Backend convergence required no schema change, so no `V20260715_*` file was created. The retired 28-file application resource SQL directory was deleted; runtime startup and tests succeed without it.

## Task 11 migration note

Changing the management client from `/web` and retired action routes to the unified `/api` contract is an HTTP-client configuration change only. It adds no table, column, seed, or data conversion, so Task 11 creates no migration and changes none of the 74 protected historical files.

## Task 3 persistence decision

No schema migration is required. Existing management `employee`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`, and `sys_user_permission` tables and mappers are the canonical persistence model for both clients.
