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

## Routine release

Synchronize release-owned files with `scripts/sync-release-files.sh`, then run:

```bash
bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
NO_CACHE=1 bash scripts/restart.sh
bash scripts/smoke-test.sh
```

`restart.sh` builds the local backend image, stops backend writes, runs the sole migration entry and recreates only `backend` and `nginx`. A migration failure leaves the backend stopped. It never recreates MySQL or Redis and does not pull images unless explicitly requested.

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

WeChat, Kuaidi100, OSS, SMS, RabbitMQ and XXL-JOB remain disabled by default. Enable each integration only after its credentials and health checks are configured. Optional services must not create a second Hive backend process.

## Acceptance

- `docker compose ps` shows exactly one Hive business container: `hive-backend`.
- Migration history has no failed records.
- Nginx and backend health checks pass.
- Management web and mini-program call only `/api/**`.
- Smoke tests cover authentication, orders, approvals, inventory, notifications and printing.
- Logs contain no duplicate bean, route, listener or scheduler registration errors.
