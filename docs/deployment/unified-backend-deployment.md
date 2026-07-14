# Unified Backend Deployment

## Target

The release topology will contain one business service named `backend`, one container named `hive-backend`, one executable management JAR, and nginx routing `/api/**` to port 8080. Cutover, health verification, snapshot, and rollback procedures will be completed with the deployment source.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | COMPLETE |

Configure `wechat.mini-program.enabled`, `wechat.mini-program.app-id`, and `wechat.mini-program.app-secret` to enable mini WeChat login. Clients must use the namespaced admin/mini routes; no ambiguous login alias is deployed.
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
| notification | PLANNED |
| attendance | PLANNED |
| migration | PLANNED |
| deployment | PLANNED |

## Task 6 deployment note

The unified backend JAR now serves inventory, quality, and installation APIs from the same process and `/api` context. No additional backend container, scheduler instance, or migration entry point is introduced by this task.

## Task 7 deployment note

The unified backend JAR now serves customer, document, equipment, label-template, receipt-print, and generic print-task APIs from the same process and `/api` context. No extra backend container, print service container, scheduler instance, or migration entry point is introduced by this task.
## Permission runtime

The unified process has one Permission Catalog V3 bean and one authenticated-route tenant-context initializer. Deployments must not seed wildcard, alias, prefix, or legacy permission codes; only exact assignable V3 leaves are effective.
