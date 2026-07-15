# Unified Backend Deployment

## Target

The release topology will contain one business service named `backend`, one container named `hive-backend`, one executable management JAR, and nginx routing `/api/**` to port 8080. Cutover, health verification, snapshot, and rollback procedures will be completed with the deployment source.

## Module status

| Module | Status |
| --- | --- |
| foundation | COMPLETE |
| permission | COMPLETE |
| auth | COMPLETE |

Configure `wechat.mini-program.enabled`, `wechat.mini-program.app-id`, and `wechat.mini-program.app-secret` to enable mini WeChat login. Clients must use the namespaced admin/mini routes; no ambiguous login alias is deployed.
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
| deployment | IN PROGRESS (SINGLE-SERVICE SOURCE COMPLETE) |

## Task 6 deployment note

The unified backend JAR now serves inventory, quality, and installation APIs from the same process and `/api` context. No additional backend container, scheduler instance, or migration entry point is introduced by this task.

## Task 7 deployment note

The unified backend JAR now serves customer, document, equipment, label-template, receipt-print, and generic print-task APIs from the same process and `/api` context. No extra backend container, print service container, scheduler instance, or migration entry point is introduced by this task.

## Task 8 deployment note

The single backend process owns all notification, SMS, WeChat subscription, attendance, statistics, maintenance-job, and operation-log consumer registrations. Configure one XXL-JOB executor with app name `hive-backend`, one executor port, one log path, and the single `XXL_JOB_ENABLED` flag. Required unique handlers are `attendanceDailyStatJob`, `inventoryDailyStatJob`, `notificationClosedLoopJob`, `runtimeStabilityAuditJob`, `dbCapacityReportJob`, and `dbCleanupJob`.

Optional WeChat subscription delivery uses `WECHAT_SUBSCRIBE_ENABLED`, `WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID`, and the three template field-key variables in addition to the shared mini-program app ID/secret. The operation-log RabbitMQ queue has one listener declaration. A second scheduler executor, notification consumer, attendance scheduler, or mini-backend listener must not be deployed.

## Task 9 deployment note

The executable source boundary is now exclusively `my.hive`, with one `HiveApplication`, one Controller registry, six unique XXL-JOB handler names, and one operation-log Rabbit listener. There is no legacy package scanning and no runtime dependency on either retired backend source tree. Authentication audit is registered in the same process for both admin and mini-program entry points and suppresses credential arguments.

Task 9 changes no environment-variable name, container topology, migration command, or database schema. The deployable JAR and Docker/deployment-directory cutover are intentionally deferred to the remaining migration, client-route, and deployment tasks; do not publish the Task 9 intermediate artifact as the final release package.

## Task 10 deployment note

The repository migration source is imported byte-for-byte from the approved deployment package. Run `bash scripts/migrate-db.sh` from the deployment root; it owns preflight, backup verification, manifest execution, checksum drift rejection, and post-migration schema verification. Its required safety helpers are stored beside it, while internal baseline/shadow utilities remain below `db-migrations/scripts` and are not additional public migration entry points.

The current manifest contains 74 ordered migrations and has a matching full checksum snapshot. Task 10 changes no schema and does not require an online migration run. The deployment topology and remaining operational scripts will be rewritten for the single backend in Task 12 before release synchronization.

## Task 11 deployment note

The management production build now targets `/api`, matching the mini-program public prefix and the single Spring Boot context path. Local Vite proxying uses `http://localhost:8080`; production nginx must therefore expose only `/api/**` to the one `hive-backend` process. No `/web` compatibility location should be retained during Task 12.

The client route gate and all management UI Node tests pass, and the Vite production bundle builds successfully. This verifies the client contract but is not yet a deployable release package; Compose, nginx, health checks, restart/smoke scripts, and deployment-directory synchronization remain Tasks 12-14.

## Task 12 single-service deployment source

`deploy/docker-compose.yml` defines the one business service `backend` (`container_name: hive-backend`). It builds `deploy/backend/Dockerfile`, runs the release JAR as `/app/app.jar`, exposes only container port 8080, mounts `logs/backend` and `uploads`, and has one health check. Optional RabbitMQ and XXL-JOB admin use Compose profiles; the application always has one operation-log queue selection and one executor name (`hive-backend`).

`deploy/nginx/conf.d/hive.conf` routes `/api/**` only to `backend:8080`. TLS certificate files, `.env`, JARs, UI output, data volumes, logs, uploads, reports, and snapshots are runtime inputs excluded by `deploy/.gitignore`. `.env.example` uses the new `WECHAT_MINI_PROGRAM_*` and `WECHAT_SUBSCRIBE_*` property names and maps all environment references in `application.yaml` and `application-prod.yaml` into the single service.

Operational entry points are:

- `scripts/start.sh` and `scripts/restart.sh` for migration-aware startup/cutover;
- `scripts/check-deploy-health.sh`, `scripts/smoke-test.sh`, and `scripts/verify-low-cost-mode.sh` for validation;
- `scripts/inspect-backend-artifact.sh` and `scripts/verify-release-integrity.sh` for one-JAR enforcement;
- `scripts/create-release-snapshot.sh` and `scripts/rollback-release.sh` for file-level release rollback;
- `scripts/stop.sh` and `scripts/logs.sh` for operations;
- `scripts/migrate-db.sh` as the sole database migration entry in an assembled package.

The topology contract, YAML duplicate-key/structure validation, complete application-variable mapping, retired-reference scan, and Bash syntax checks pass. Docker CLI is not installed on the current workstation, so `docker compose --env-file .env.example config -q` remains a mandatory release-host gate before start or restart. Task 13 still needs to build and exercise the unique JAR; Task 14 still needs to assemble and synchronize the final deployment directory.

## Permission runtime

The unified process has one Permission Catalog V3 bean and one authenticated-route tenant-context initializer. Deployments must not seed wildcard, alias, prefix, or legacy permission codes; only exact assignable V3 leaves are effective.
