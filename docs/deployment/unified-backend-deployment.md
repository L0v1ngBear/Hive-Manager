# Unified Backend Deployment

## Target

The release topology will contain one business service named `backend`, one container named `hive-backend`, one executable management JAR, and nginx routing `/api/**` to port 8080. Cutover, health verification, snapshot, and rollback procedures will be completed with the deployment source.

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
## Permission runtime

The unified process has one Permission Catalog V3 bean and one authenticated-route tenant-context initializer. Deployments must not seed wildcard, alias, prefix, or legacy permission codes; only exact assignable V3 leaves are effective.
