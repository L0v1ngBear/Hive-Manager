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
| inventory | PLANNED |
| quality | PLANNED |
| installation | PLANNED |
| customer | PLANNED |
| document | PLANNED |
| equipment | PLANNED |
| print | PLANNED |
| notification | PLANNED |
| attendance | PLANNED |
| migration | PLANNED |
| deployment | PLANNED |
## Task 3 persistence decision

No schema migration is required. Existing management `employee`, `sys_role`, `sys_permission`, `sys_user_role`, `sys_role_permission`, and `sys_user_permission` tables and mappers are the canonical persistence model for both clients.
