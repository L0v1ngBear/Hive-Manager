# Unified Backend Migrations

## Contract

Historical versioned SQL is immutable. Any convergence schema change must be a new `V*.sql` migration and must update the migration manifest. Production cutover does not preserve retired business data, tokens, caches, or API compatibility behavior. The single migration command and verified baseline will be established in the migration task.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | PLANNED |
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
