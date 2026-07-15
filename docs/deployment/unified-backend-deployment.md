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
| notification | COMPLETE |
| attendance | COMPLETE |
| migration | PLANNED |
| deployment | PLANNED |

## Task 6 deployment note

The unified backend JAR now serves inventory, quality, and installation APIs from the same process and `/api` context. No additional backend container, scheduler instance, or migration entry point is introduced by this task.

## Task 7 deployment note

The unified backend JAR now serves customer, document, equipment, label-template, receipt-print, and generic print-task APIs from the same process and `/api` context. No extra backend container, print service container, scheduler instance, or migration entry point is introduced by this task.

## Task 8 deployment note

The single backend process owns all notification, SMS, WeChat subscription, attendance, statistics, maintenance-job, and operation-log consumer registrations. Configure one XXL-JOB executor with app name `hive-backend`, one executor port, one log path, and the single `XXL_JOB_ENABLED` flag. Required unique handlers are `attendanceDailyStatJob`, `inventoryDailyStatJob`, `notificationClosedLoopJob`, `runtimeStabilityAuditJob`, `dbCapacityReportJob`, and `dbCleanupJob`.

Optional WeChat subscription delivery uses `WECHAT_SUBSCRIBE_ENABLED`, `WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID`, and the three template field-key variables in addition to the shared mini-program app ID/secret. The operation-log RabbitMQ queue has one listener declaration. A second scheduler executor, notification consumer, attendance scheduler, or mini-backend listener must not be deployed.
## Permission runtime

The unified process has one Permission Catalog V3 bean and one authenticated-route tenant-context initializer. Deployments must not seed wildcard, alias, prefix, or legacy permission codes; only exact assignable V3 leaves are effective.
