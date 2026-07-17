# Hive One-Command Release Design

## Goal

Provide a clean upload package that can be released with one command after it is uploaded to the server. Operators must not need to assemble `RELEASE_SOURCE_DIR`, `RELEASE_TARGET_DIR`, synchronization, restart, or smoke-test commands manually.

## Package Contract

The upload package contains a root-level `publish.sh`. The package contains release artifacts only and must not contain `.env`, TLS certificates, database files, uploads, backups, logs, or mini-program source files. Runtime-owned files remain under `/root/hive` and are preserved by synchronization.

The operator uploads the package to any server directory and runs:

```bash
cd /path/to/uploaded-release
bash publish.sh
```

## Release Flow

`publish.sh` resolves its own directory as the immutable release source and uses `/root/hive` as the default runtime target. It then:

1. Verifies that it is not running from the runtime target.
2. Calls `scripts/sync-release-files.sh` with explicit source and target paths.
3. Changes into `/root/hive`.
4. Calls `scripts/restart.sh`.
5. Relies on the restart smoke gate to require both the management web home and unified API routes to pass.

`HIVE_RUNTIME_DIR` may override `/root/hive` for controlled testing. No option may skip synchronization, migration safety, backup, static-file permission normalization, or smoke verification.

## Failure Handling

The script uses strict shell mode and stops on the first failed command. It prints the resolved source and target before changing server state. A failed synchronization does not invoke restart. A failed migration leaves the backend stopped according to the existing restart contract. A failed management web or API smoke check returns a non-zero exit code.

## Verification

Repository tests must verify that `publish.sh` derives its source from its own location, calls the allowlisted synchronization script, targets `/root/hive` by default, and invokes only the existing restart entrypoint. Deployment-package verification must confirm that `publish.sh` is present and that forbidden runtime paths are absent.
