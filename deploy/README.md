# Hive single-backend deployment source

This directory is the version-controlled deployment template. A release assembly copies exactly one executable JAR to `backend/hive-backend.jar`, the management UI build to `management-ui/dist`, the repository `db-migrations` tree to `db-migrations`, and the release metadata to `RELEASE_BUILD_INFO.txt`.

## Required local files

The following files are intentionally excluded from Git:

- `.env`, created from `.env.example` with production secrets;
- `backend/hive-backend.jar`, produced by the release build;
- `management-ui/dist`, produced by Vite;
- `nginx/certs/hellohive.top.pem` and `nginx/certs/hellohive.top.key`;
- database, Redis, RabbitMQ, upload, log, report, and snapshot data.

## Commands

```bash
# Run this against the secret-free staging directory before uploading.
HIVE_RELEASE_ROOT=/absolute/path/to/upload-staging bash scripts/verify-upload-package.sh

# Run these only inside the server-owned runtime directory.
test -f .env || cp .env.example .env
bash scripts/check-deploy-health.sh
bash scripts/verify-release-integrity.sh
bash scripts/start.sh
bash scripts/restart.sh
bash scripts/smoke-test.sh
bash scripts/smoke-unified-backend.sh
bash scripts/reset-fresh-business-data.sh
bash scripts/rollback-release.sh
```

`verify-upload-package.sh` and `check-deploy-health.sh` intentionally enforce opposite secret boundaries. The upload gate rejects `.env`, TLS keys/certificates, persistent volumes, uploads, logs, reports, and snapshots. The runtime gate requires the server-owned `.env` and TLS files. Never bypass either gate by copying the whole runtime directory as an upload package.

`verify-release-integrity.sh` validates the backend JAR, the deterministic management UI tree hash and file count, the UI entry file, the migration manifest and checksums, retired frontend contracts, and resolvable Git commits when `SOURCE_REPOSITORY_ROOT` is supplied on the build host.

`scripts/migrate-db.sh` is the only migration command in an assembled deployment package. It executes the immutable manifest under `db-migrations` and rejects checksum drift.

The Compose topology contains one business service named `backend`, one container named `hive-backend`, and one `/api/**` nginx upstream. RabbitMQ and XXL-JOB admin are optional profiles; the application still owns only one operation-log listener registration and one executor configuration.

Never commit real secrets, certificates, release JARs, generated UI assets, persistent volumes, reports, or snapshots from this directory.
