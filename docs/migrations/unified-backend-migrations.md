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
| customer | PLANNED |
| document | PLANNED |
| equipment | PLANNED |
| print | PLANNED |
| notification | PLANNED |
| attendance | PLANNED |
| migration | PLANNED |
| deployment | PLANNED |

## Task 6 migration note

Inventory, quality, and installation convergence is a Java package and API adapter consolidation. It does not require modifying historical SQL or adding a new versioned migration; the existing `cloth`, `inventory_record`, `inventory_setting`, `bad_product_record`, and `installation_task` table contracts remain canonical.
## Task 3 persistence decision

No schema migration is required. Existing management `employee`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`, and `sys_user_permission` tables and mappers are the canonical persistence model for both clients.
