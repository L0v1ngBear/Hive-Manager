# baseline

This folder keeps the schema-only baseline used by rebuild and shadow-schema verification.

Current immutable file:

```text
hive_schema_baseline_v2.sql
```

Rules:
- Keep schema only: `CREATE TABLE`, indexes, triggers, views, and stored routines when needed.
- Do not include business seed data, test data, `CREATE DATABASE`, or `USE hive`.
- Online changes must go through `db-migrations/migration_manifest.txt`.
- The active permission catalog is stored separately in `../seeds/system_permission_catalog_v3.sql`.
- A normal restart never imports a baseline. Only `initialize-fresh-database.sh` may initialize an empty database.
- Encoding must be UTF-8 without BOM.
