# Unified API Catalog

## Contract

All public business routes will use `/api/**`; no `/web/**` compatibility route will remain. Authentication entry points will be separated into `/api/auth/admin/**` and `/api/auth/mini/**` while sharing session, tenant, permission, and user-state services. Domain endpoint matrices will be added as each capability converges.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | COMPLETE |

Authentication routes are `POST /api/auth/admin/login`, `POST /api/auth/admin/scan-login/session`, `GET /api/auth/admin/scan-login/status`, `POST /api/auth/admin/scan-login/confirm`, `POST /api/auth/mini/login`, `POST /api/auth/mini/wechat-login`, `GET /api/auth/me`, and `POST /api/auth/logout`. Reset, initial-password, and organization-join routes are retained only below `/api/auth/admin/**`; `/api/auth/login` does not exist.
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
