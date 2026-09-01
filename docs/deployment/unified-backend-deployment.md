# Unified backend deployment

## Runtime topology

Hive has one business service named `backend`, one container named `hive-backend`, one executable backend JAR and one management web build. Nginx routes `/api/**` to `backend:8080`. MySQL and Redis stay running during a routine application release when the existing Compose network already matches this release. A controlled network upgrade is the documented exception described below.

The mini-program and management web use the same backend. There are no `management-backend-1`, `backend-1`, `/web/**` compatibility routes or dual-backend startup paths in the current release.

Nginx is the only externally published HTTP entry. The backend exposes port
8080 only inside `hive-net`. Compose assigns that network
`HIVE_DOCKER_SUBNET=172.30.0.0/24` by default and passes the same subnet, plus
loopback, to the backend trusted-proxy list. Before first startup, compare this
subnet with host, LAN, cloud, and VPN routes. If it overlaps, change the single
`HIVE_DOCKER_SUBNET` value in the server-owned `.env`; Compose then updates both
IPAM and proxy trust together. Do not add general RFC1918 ranges or publish the
backend port directly.

## Required release contents

- `backend/hive-backend.jar`
- `management-ui/dist/`
- `db-migrations/`
- `nginx/`
- `scripts/`
- `publish.sh`
- `docker-compose.yml`
- `.env.example`
- `RELEASE_BUILD_INFO.txt`

Production `.env`, certificates, database/cache files, uploads, logs and backups are runtime-owned and must not be overwritten by a release upload.

## Mini-program synchronization

The WeChat mini-program source is maintained separately at
`D:\productHiveFrontend\client`; it is not copied into the server release directory.
The server release continues to contain one `hive-backend` process only. Both clients
call the same `/api/**` contract, while their authentication entry points remain
channel-specific: the management web uses `/api/auth/admin/**` and the mini-program
uses `/api/auth/mini/**`.

For a change that affects a shared API, deploy and verify the backend contract before
submitting the corresponding mini-program build in WeChat DevTools. At minimum,
verify `/api/health`, mini-program login and the affected authenticated business
route against the same deployed build identity. The mini-program production request
host is configured in its own `client/utils/request.js`; its production HTTPS domain
must be added to the WeChat request-domain allow list. Do not add the mini-program
source, its local configuration, or WeChat credentials to the server deployment
package.

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

Before changing services, `restart.sh` renders the effective Compose configuration, so a top-level `name` or `COMPOSE_PROJECT_NAME` override is respected, and resolves the actual `hive-net` network name. It compares the existing network's IPAM subnet with `HIVE_DOCKER_SUBNET` and checks the release-owned network configuration marker.

- If the network is absent, startup proceeds normally and recreates only `backend` and `nginx`.
- If the subnet and configuration marker match, the routine path recreates only `backend` and `nginx`; MySQL, Redis and optional services remain running.
- If the subnet differs, or the network is from a legacy release and has no current configuration marker, the script performs a controlled full-project network migration. After database migration it runs `docker compose down --remove-orphans` and then unfiltered `docker compose up -d`, restarting every service enabled by the current `.env` and `COMPOSE_PROFILES`, including RabbitMQ or XXL-JOB when enabled. Expect downtime for the entire Compose project while its network and containers are recreated.

The controlled migration never passes `-v` or `--volumes`. Named volumes, bind-mounted MySQL/Redis data, uploads, logs and other persistent data are preserved. It fails before migration if the network has containers owned by another Compose project, or if Docker cannot inspect the network. If teardown or recreation fails, follow the printed recovery guidance; do not delete volumes to retry.

After either startup path, the script inspects the live network again and requires its actual subnet to equal `HIVE_DOCKER_SUBNET` before starting container health checks. If the default subnet collides with a host, LAN, cloud or VPN route, first choose an unused CIDR and change the single `HIVE_DOCKER_SUBNET` value in the server-owned `.env`; the next restart intentionally takes the full-project migration path. Never silence a mismatch by broadening `TRUSTED_PROXY_CIDRS`.

The script builds the local backend image, stops backend writes and runs the sole migration entry before bringing up the selected topology. A database migration failure leaves the backend stopped. It does not pull images unless explicitly requested.

## Independent MySQL cutover entry

`HIVE_DATABASE_MODE=COMPOSE` is the default and continues to use the bundled
MySQL container. The explicit `HIVE_DATABASE_MODE=EXTERNAL` entry is for a
separately operated MySQL 8 instance, including Alibaba Cloud RDS MySQL. Set the `HIVE_DATABASE_JDBC_URL`, the
application username/password, and the independent admin host, port and
credentials in the server-owned `.env`. The backend uses the JDBC/application
fields; the release host uses the admin fields for backup, migration and schema
verification. The Docker network must reach the JDBC host and the release host
must provide the `mysql` and `mysqldump` clients.

Restore or copy a complete Hive database into the independent MySQL instance
before changing modes. Do not cut over an empty or partial schema. Then run:

```bash
bash scripts/verify-external-mysql.sh
bash scripts/restart.sh
```

The first command is read-only against MySQL and requires a `READY` Hive schema.
The second command is the existing guarded release flow, now routed to the
external connection: it backs up, migrates and verifies that database, then
starts only Redis, backend and Nginx. It does not start or write the bundled
`mysql` service. Local `xxl-job-admin` is not started in EXTERNAL mode; use a
separately managed scheduler endpoint if XXL-JOB is enabled. No automatic data
copy occurs when switching in either direction.

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

WeChat, Aliyun Market logistics, APISpace logistics, OSS, SMS, RabbitMQ and XXL-JOB remain disabled by default. Logistics defaults to `LOGISTICS_PROVIDER=aliyun-market`, with `ALIYUN_MARKET_LOGISTICS_ENABLED=false` and a blank `ALIYUN_MARKET_LOGISTICS_APPCODE` in `.env.example`. Set the AppCode only in the server-owned `.env`; the deployment package and source configuration must contain only environment-variable mappings. The legacy APISpace provider remains available by explicitly setting `LOGISTICS_PROVIDER=apispace`.

For website WeChat quick login, create and approve a website application in WeChat Open Platform, enable website login, and configure its callback to the exact HTTPS address `https://<public-domain>/api/auth/admin/wechat-login/callback`. Then set only the server-owned `.env` values `WECHAT_WEB_LOGIN_ENABLED=true`, `WECHAT_WEB_LOGIN_APP_ID`, `WECHAT_WEB_LOGIN_APP_SECRET`, and `WECHAT_WEB_LOGIN_CALLBACK_URI`. The website application credentials are separate from the mini-program credentials. Keep `WECHAT_WEB_LOGIN_FRONTEND_PATH=/login` unless the public login route changes.

File storage uses `FILE_STORAGE_PROVIDER=local` by default, so existing local uploads remain unchanged. To select `aliyun-oss`, set `ALIYUN_OSS_ENABLED=true` and provide the endpoint, bucket, access key ID and access key secret. The deployment health check rejects an enabled Aliyun Market integration without its AppCode, an enabled APISpace integration without its token, and an OSS provider without all required OSS settings. Optional services must not create a second Hive backend process.

## Acceptance

- `docker compose ps` shows exactly one Hive business container: `hive-backend`.
- Migration history has no failed records.
- Nginx and backend health checks pass.
- Management web and mini-program call only `/api/**`.
- Smoke tests cover authentication, orders, approvals, inventory, notifications and printing.
- Logs contain no duplicate bean, route, listener or scheduler registration errors.
