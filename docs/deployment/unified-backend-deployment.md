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
| deployment | PACKAGE SYNCED (RELEASE-HOST DOCKER GATE OPEN) |

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

The topology contract, YAML duplicate-key/structure validation, complete application-variable mapping, retired-reference scan, and Bash syntax checks pass. Docker CLI is not installed on the current workstation, so `docker compose --env-file .env.example config -q` remains a mandatory release-host gate before start or restart. Task 14 still needs to assemble and synchronize the final deployment directory.

## Task 13 artifact and smoke verification

`mvnw clean package` passed 186 tests and produced exactly one `target/*.jar`: `hive-backend-0.0.1-SNAPSHOT.jar`. Inspection found one `HiveApplication`, one build-info resource, one admin auth Controller, one mini auth Controller, one health Controller, and zero `my.management`/`my.hive_back` classes. The Task 13 verification artifact SHA-256 was `B0B291B6C4473BA5345AF6BF4733AC57451573594965967BC2DB26146758AFDD`; Task 14 must rebuild and record the final release hash after the source commit.

The JAR started successfully with Java 21 on local MySQL/Redis/RabbitMQ while schedulers, maintenance, outbound messaging, and operation logging were disabled. `smoke-unified-backend.sh` verified the health route, both login rejection paths, current user, employees, orders, approval, inventory, notifications, and print tasks. All responses reported build `hive-backend:0.0.1-SNAPSHOT:2026-07-15T03:46:47.379Z` and process instance `76ab10e7-6788-4c33-8536-f84d42aab9c9`; startup logs contained no duplicate Bean, mapping, listener, or handler failure. The temporary process was stopped after verification.

Container startup remains untested on this workstation because Docker CLI is absent. Release-host Compose expansion, nginx TLS validation, container health, and full deployment smoke remain mandatory Task 14 gates.

## Task 14 synchronized release package

The deployment directory `C:\Users\HUAWEI\Desktop\hive部署_全新配置` has been synchronized from the unified repository source. It contains one JAR (`backend/hive-backend.jar`, SHA-256 `95d7c03998700a9d9ae494de017442269d0b1123c7e4ec904f05bb16d9fff33c`), 73 clean UI files, 74 migrations, one backend service, and no retired application directory or runtime reference. The real `.env`, database/cache/message data, uploads, certificates, and ACME state were preserved; obsolete code/configuration was deleted after snapshots.

Verified rollback assets are outside the deployment package:

- application snapshot: `C:\Users\HUAWEI\Desktop\hive部署_发布快照\pre-unify-dual-backend-20260715_115134.zip` (`971d11af...ccb4c1`);
- documentation snapshot: `C:\Users\HUAWEI\Desktop\hive部署_发布快照\pre-unify-docs-20260715_115134.zip` (`60dc301b...9d5e5`);
- secret environment backup: `C:\Users\HUAWEI\Desktop\hive部署_发布快照\pre-unify-dual-backend-20260715_115134.env` (`64fb0ada...18cd9`).

### Release-host cutover

1. Verify the three snapshot hashes above, restrict access to the `.env` backup, and run the approved database backup plus `scripts/verify-latest-backup.sh`.
2. With the old Compose file still active, stop both old application services and nginx so no writes enter during package replacement. Do not delete MySQL, Redis, RabbitMQ, upload, certificate, or ACME data.
3. Upload/synchronize the new package and restore the production `.env`. Confirm it contains the unified WeChat, auth, executor, logging, and privacy keys and no retired runtime keys.
4. Run `docker compose config`, `docker compose run --rm --no-deps nginx nginx -t`, `bash scripts/check-deploy-health.sh`, and `bash scripts/verify-release-integrity.sh`.
5. For the approved clean-business-data launch, first run `bash scripts/reset-fresh-business-data.sh` in preview mode, then run `CONFIRM_FRESH_BUSINESS_RESET=YES bash scripts/reset-fresh-business-data.sh`. Never skip its verified backup.
6. Run the only migration entry: `bash scripts/migrate-db.sh`.
7. Start with `bash scripts/start.sh`. Confirm `docker compose ps` shows one `hive-backend` business container and no orphan application container.
8. Run `bash scripts/smoke-unified-backend.sh` and verify health, admin login rejection/success fixture, mini/WeChat login rejection/success fixture, current user, employees, orders, approvals, inventory, notifications, and print tasks return the same build and instance headers.
9. Verify printing/attachment behavior with approved fixtures, one XXL-JOB executor registration and six handler names when scheduling is enabled, one RabbitMQ listener when selected, and logs free of duplicate Bean/mapping/listener/handler errors.
10. Monitor HTTP error rate, database migration history, scheduler registration, notification delivery, printing, and resource use before reopening writes to all users.

### Rollback

1. Stop `backend` and nginx immediately and preserve its logs/report evidence.
2. If the reset or any migration ran, restore the verified pre-release database backup. Do not write or execute a down migration.
3. Clear the dedicated Hive Redis namespace/database and restore the pre-release uploads snapshot if business uploads were removed.
4. Extract the application snapshot back into the deployment directory and restore the separately protected pre-unify `.env`; restore the documentation snapshot when the old runbook is needed.
5. Restore the old Compose/nginx files and two old JAR directories from the snapshot, then start the two old application services with their original infrastructure containers.
6. Execute the archived old smoke suite and verify both old upstreams, login paths, orders, approvals, notifications, printing, schedulers, and logs before reopening traffic.

The workstation passed static deployment health, full artifact hash verification, migration checksum/baseline validation, topology parsing, and retired-reference scans. Docker CLI is unavailable here, so the Compose/nginx/container steps above remain blocking release-host checks rather than presumed successes.

## Permission runtime

The unified process has one Permission Catalog V3 bean and one authenticated-route tenant-context initializer. Deployments must not seed wildcard, alias, prefix, or legacy permission codes; only exact assignable V3 leaves are effective.
