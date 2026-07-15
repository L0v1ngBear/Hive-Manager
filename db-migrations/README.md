# Hive 数据库完整迁移体系

目的：把“临时补字段”改成“可追踪、可回滚、可验证”的线上数据库升级流程。后续不要再直接在服务器零散执行 SQL，统一走本目录。

## 目录职责

- `baseline/`：放 schema-only 基线，例如 `hive_schema_baseline.sql`。用于新服务器初始化、线上库混乱时重建影子库。
- `migrations/`：放后续版本化迁移 SQL，只允许新增，不允许修改已执行过的历史文件。
- `migration_manifest.txt`：迁移清单，按顺序列出要执行的 SQL。
- `scripts/preflight-online.sh`：线上迁移前预检 MySQL、磁盘、权限和 DDL 能力。
- `scripts/backup-online.sh`：迁移前备份线上 `hive` 数据库。
- `scripts/run-versioned-migrations.sh`：按清单执行版本迁移，并写入 `schema_migration_history`。
- `scripts/import-baseline-to-shadow.sh`：把 schema-only 基线导入影子库，默认不覆盖线上 `hive`。
- `scripts/verify-online-schema.sh`：校验迁移历史、失败记录和影子库/目标库结构差异。
- `scripts/export-local-baseline.ps1`：本地导出标准库的 PowerShell 模板。
- `scripts/diagnose-mysql-storage.sh`：非破坏性诊断 MySQL 进程活着但 DDL 失败的问题。
- `scripts/rebuild-mysql-from-baseline.sh`：带强确认的 MySQL 数据目录重建脚本，默认不允许执行。

## 推荐流程

1. 先确认本地 Navicat 的 `local/hive` 是当前标准库。
2. 在本机执行 `scripts/export-local-baseline.ps1` 导出 schema-only 基线到 `baseline/hive_schema_baseline.sql`。
3. 把整个 `migration-system` 上传到服务器 `/root/hive/db-migrations`。
4. 服务器先跑预检：`bash db-migrations/scripts/preflight-online.sh`。
5. 服务器先备份：`bash db-migrations/scripts/backup-online.sh`。
6. 如果只是补版本字段：`bash db-migrations/scripts/run-versioned-migrations.sh`。
7. 如果线上库已经乱了：先导入影子库验证，`TARGET_DATABASE=hive_shadow RESET_TARGET=YES bash db-migrations/scripts/import-baseline-to-shadow.sh`。
8. 验证影子库：`BASELINE_DATABASE=hive_shadow bash db-migrations/scripts/verify-online-schema.sh`。
9. 确认无误后，再决定是否把影子库作为新的线上库或手动导入到 `hive`。

如果预检阶段出现 `Got error 168` 或 `Failed to create schema directory`，先执行 `bash db-migrations/scripts/diagnose-mysql-storage.sh`，不要继续迁移。

## 关键原则

- 业务运行账号 `hive_app` 不做 DDL，DDL 统一由迁移脚本用 root 执行。
- 任何已执行成功的迁移 SQL 禁止改内容；要变更就新增一个版本文件。
- 完整重建前必须先备份线上库，不允许直接 `DROP DATABASE hive`。
- 默认脚本不会覆盖线上 `hive`；覆盖必须显式设置确认变量。
- 中文文件统一 UTF-8 无 BOM。
