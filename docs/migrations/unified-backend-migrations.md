# Unified Backend Migrations

## Contract

Historical versioned SQL is immutable. Any convergence schema change must be a new `V*.sql` migration and must update the migration manifest. Production cutover does not preserve retired business data, tokens, caches, or API compatibility behavior. The single migration command and verified baseline will be established in the migration task.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | COMPLETE |

Task 4 requires no schema change. Both clients use the existing user `auth_version` for versioned sessions and logout invalidation.
| order | PLANNED |
| approval | PLANNED |
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
| migration | PLANNED |
| deployment | PLANNED |

## Task 6 migration note

Inventory, quality, and installation convergence is a Java package and API adapter consolidation. It does not require modifying historical SQL or adding a new versioned migration; the existing `cloth`, `inventory_record`, `inventory_setting`, `bad_product_record`, and `installation_task` table contracts remain canonical.

## Task 7 migration note

Customer, document, equipment, label-template, receipt-print, and print-task convergence is a Java package/API consolidation. It does not modify historical SQL and does not require a new migration; the existing customer, document, equipment, label template, outbound receipt, and `print_task` table contracts remain canonical.

## Task 8 migration note

Task 8 does not modify an executed historical SQL file. The unified implementation consumes the existing `notification_record`, `enterprise_announcement`, `attendance_record`, `tenant_attendance_rule`, `tenant_attendance_location`, `employee_attendance_location`, `attendance_statics`, `inventory_statics`, and `wechat_subscribe_user` contracts already present in the approved deployment baseline/version history. Task 10 will import that history unchanged into the repository and verify its manifest checksums; any subsequently discovered schema delta must be introduced as a new version file.
## Task 3 persistence decision

No schema migration is required. Existing management `employee`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`, and `sys_user_permission` tables and mappers are the canonical persistence model for both clients.
