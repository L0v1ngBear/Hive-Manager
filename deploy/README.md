# Hive unified deployment

This directory is the version-controlled release template for one Spring Boot backend, one management UI, MySQL, Redis and Nginx. RabbitMQ and XXL-JOB are optional and disabled by default.

## Release boundary

Release-owned files are synchronized with `scripts/sync-release-files.sh`. Runtime-owned paths are never uploaded or deleted:

- `.env`
- `mysql/data`
- `redis/data`
- `nginx/certs`
- `uploads`
- `backups`

The upload staging directory must contain no secrets, certificates or persistent data. Run `scripts/verify-upload-package.sh` before uploading.

## First installation

`V20260717_001_order_multi_shipment.sql` is a clean-launch destructive contract. Before this release is deployed, back up if required and clear all old business data so the target database is empty. Do not preserve or backfill `sales_order.express_company` / `sales_order.express_no`; this release drops those columns and has no compatibility reads.

1. Synchronize the release into the server runtime directory.
2. Create `.env` once, without overwriting a server-owned file, then set production secrets:

```bash
test -f .env || cp .env.example .env
```

3. Start MySQL and Redis: `docker compose up -d mysql redis`.
4. Initialize the confirmed empty database:

```bash
CONFIRM_FRESH_DATABASE_INITIALIZATION=YES \
  bash db-migrations/scripts/initialize-fresh-database.sh
```

5. Start and verify: `bash scripts/start.sh`.

Fresh initialization creates only `TENANT_001`, one administrator that must change its temporary password, the active permission catalog, and the current schema. The immutable baseline represents migrations through `V20260716_001`; the importer registers that cutoff and the versioned runner executes `V20260717_001`. It does not create test data, `TENANT_002`, AI permissions or old dual-order permissions.

## Normal release

When the server uses only `/root/hive`, upload the release contents into that directory and run:

```bash
cd /root/hive
bash publish.sh
```

The direct-runtime wrapper removes stale hashed management UI files using the signed release manifest, then runs the guarded restart entrypoint. Runtime-owned `.env`, certificates, database files, uploads and backups are preserved. A separate staging directory remains optional; when used, the wrapper synchronizes release-owned files into `/root/hive` with `rsync --delete` before restarting.

The equivalent lower-level commands remain available for diagnosis:

```bash
HIVE_RELEASE_ROOT=/root/hive bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
bash scripts/restart.sh
```

`restart.sh` validates the release, builds the backend image, stops backend writes, backs up and migrates the managed database, then recreates only `backend` and `nginx`. It does not recreate MySQL or Redis. A failed migration leaves the backend stopped.

## Independent MySQL cutover

The default is `HIVE_DATABASE_MODE=COMPOSE`, which retains the bundled MySQL
container and existing data path. To deliberately use a separately operated
MySQL 8 instance such as Alibaba Cloud RDS MySQL, set
`HIVE_DATABASE_MODE=EXTERNAL` in the server-owned `.env`
and populate every `HIVE_DATABASE_*` field. The JDBC URL and application account
are used by `hive-backend`; the admin host/port/account are used only from the
release host for the guarded backup and migration steps. The Docker network must
be able to reach the JDBC host, and the release host must have `mysql` and
`mysqldump` installed.

Copy or restore the existing Hive database into the independent MySQL instance
first. Do not switch an empty or partial database into production. With
`HIVE_DATABASE_MODE=EXTERNAL` set, run the read-only cutover gate:

```bash
bash scripts/verify-external-mysql.sh
```

Only after it reports `READY`, perform the normal guarded restart:

```bash
bash scripts/restart.sh
```

In EXTERNAL mode, `start.sh`, `restart.sh`, backup, migration, and schema
verification use the independent MySQL connection and do not start the bundled
`mysql` service. The local `xxl-job-admin` service remains disabled in this mode;
if scheduling is enabled, point `XXL_JOB_ADMIN_ADDRESSES` at a separately managed
scheduler before cutover. Switching back requires the same backup, preflight and
restart sequence with `HIVE_DATABASE_MODE=COMPOSE`; no script copies data between
the two databases automatically.

`sync-release-files.sh` normalizes the management UI tree to directory mode `0755` and file mode `0644` so the unprivileged Nginx worker can read bind-mounted assets. The final smoke test also requires the management web home to return HTTP 200; API-only health is not sufficient.

Before the versioned runner reaches `V20260717_001`, `scripts/migrate-db.sh` reads its migration-history state. A recorded `SUCCESS` bypasses the destructive row check because that migration will not run. If the version is pending, the runtime gate permits execution only when `sales_order` can be verified to contain exactly zero rows; any rows, missing table, failed query or non-success history state stops the release and directs operators to the formal cleanup process. There is no bypass flag.

Set `PULL_IMAGES=1` only for an intentional image update. Set `NO_CACHE=1` only when Docker build cache must be bypassed.

## Database recovery

Verify a backup without changing online data:

```bash
BACKUP_FILE=/root/hive/backups/db/.../hive_....sql.gz \
  bash scripts/verify-latest-backup.sh
```

Verify restore in a shadow database first:

```bash
BACKUP_FILE=/root/hive/backups/db/.../hive_....sql.gz \
  bash db-migrations/scripts/restore-verified-backup.sh
```

Only after the shadow verification passes, repeat with `CONFIRM_DATABASE_RESTORE=YES`. The script stops backend writes and creates a new pre-restore backup before replacing the online database.

There is no automatic migration-drift repair, partial business-data reset, or file-only rollback command. Historical migration files are immutable. Recovery uses an immutable release plus a verified database backup.
