# Unified API Catalog

## Contract

All public business routes will use `/api/**`; no `/web/**` compatibility route will remain. Authentication entry points will be separated into `/api/auth/admin/**` and `/api/auth/mini/**` while sharing session, tenant, permission, and user-state services. Domain endpoint matrices will be added as each capability converges.

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
## Authorization contract

Protected endpoints accept only exact assignable Permission Catalog V3 codes. Wildcards, aliases, prefixes, legacy enum names, and dot-form codes are invalid. Both authentication channels resolve employee state and effective permissions through the same tenant-scoped pipeline.
