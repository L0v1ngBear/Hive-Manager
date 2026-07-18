# Unified backend deployment

## Runtime topology

Hive has one business service named `backend`, one container named `hive-backend`, one executable backend JAR and one management web build. Nginx routes `/api/**` to `backend:8080`. MySQL and Redis are persistent infrastructure services and are never recreated during a routine application release.

The mini-program and management web use the same backend. There are no `management-backend-1`, `backend-1`, `/web/**` compatibility routes or dual-backend startup paths in the current release.

## Required release contents

- `backend/hive-backend.jar`
- `management-web/dist/`
- `mini-program/`
- `db-migrations/`
- `nginx/`
- `scripts/`
- `docker-compose.yml`
- `.env.example`

Production `.env`, certificates, database/cache files, uploads, logs and backups are runtime-owned and must not be overwritten by a release upload.

## Fresh installation

This release's order multi-shipment schema is a clean-launch destructive contract. Before deployment, back up if required and clear all old business data so the target business database is empty. Do not attempt to preserve or migrate values from `sales_order.express_company` or `sales_order.express_no`; the release has no old-column or compatibility-read path.

1. Create the production environment file only when it is absent:

   ```bash
   test -f .env || cp .env.example .env
   ```

2. Fill every required secret and production endpoint in `.env`.
3. Start MySQL and Redis, then initialize only an empty database:

   ```bash
   CONFIRM_FRESH_DATABASE_INITIALIZATION=YES \
     HIVE_BOOTSTRAP_OWNER_PASSWORD='replace-with-a-strong-password' \
     bash db-migrations/scripts/initialize-fresh-database.sh
   ```

4. Start the application:

   ```bash
   bash scripts/start.sh
   ```

The initializer creates only `TENANT_001`, one enterprise owner, the built-in role catalog and current assignable permissions. It refuses a non-empty or incomplete database.
Its immutable schema baseline represents migrations through `V20260716_001_operation_log_table.sql`; the importer records that cutoff and the versioned runner then executes `V20260717_001_order_multi_shipment.sql`.

## Routine release

Synchronize release-owned files with `scripts/sync-release-files.sh`, then run:

```bash
bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
NO_CACHE=1 bash scripts/restart.sh
bash scripts/smoke-test.sh
```

`restart.sh` builds the local backend image, stops backend writes, runs the sole migration entry and recreates only `backend` and `nginx`. A migration failure leaves the backend stopped. It never recreates MySQL or Redis and does not pull images unless explicitly requested.

The sole migration entry enforces the `V20260717_001` clean-launch contract at runtime. When that version is still pending, `sales_order` must exist and its exact row count must be zero; a non-zero count or any inability to prove emptiness fails closed with an instruction to run the formal cleanup process. When that migration has already executed successfully, the gate does not query order rows and the migration remains skipped. No confirmation or compatibility override exists.

## Database recovery

Recovery is database-first. Verify the latest backup and restore it through a shadow database before replacing the online schema:

```bash
bash scripts/verify-latest-backup.sh
CONFIRM_DATABASE_RESTORE=YES \
  BACKUP_FILE=/absolute/path/to/verified-backup.sql.gz \
  bash db-migrations/scripts/restore-verified-backup.sh
```

Do not use file-only rollback, partial business-data reset, edited historical migrations or handwritten down migrations. Application artifacts may be rolled back only when their database contract is compatible with the restored database.

## Optional integrations

WeChat, APISpace logistics, OSS, SMS, RabbitMQ and XXL-JOB remain disabled by default. Logistics uses `LOGISTICS_PROVIDER=apispace`, with `APISPACE_LOGISTICS_ENABLED=false` and a blank `APISPACE_LOGISTICS_TOKEN` in `.env.example`. Set the token only in the server-owned `.env`; the deployment package and source configuration must contain only environment-variable mappings.

File storage uses `FILE_STORAGE_PROVIDER=local` by default, so existing local uploads remain unchanged. To select `aliyun-oss`, set `ALIYUN_OSS_ENABLED=true` and provide the endpoint, bucket, access key ID and access key secret. The deployment health check rejects an enabled APISpace integration without its token and rejects an OSS provider without all required OSS settings. Optional services must not create a second Hive backend process.

## Acceptance

- `docker compose ps` shows exactly one Hive business container: `hive-backend`.
- Migration history has no failed records.
- Nginx and backend health checks pass.
- Management web and mini-program call only `/api/**`.
- Smoke tests cover authentication, orders, approvals, inventory, notifications and printing.
- Logs contain no duplicate bean, route, listener or scheduler registration errors.
