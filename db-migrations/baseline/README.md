# baseline

This folder keeps the schema-only baseline used by rebuild and shadow-schema verification.

Recommended file:

```text
hive_schema_baseline.sql
```

Rules:
- Keep schema only: `CREATE TABLE`, indexes, triggers, views, and stored routines when needed.
- Do not include business seed data, test data, `CREATE DATABASE`, or `USE hive`.
- Online changes must go through `db-migrations/migration_manifest.txt`.
- Encoding must be UTF-8 without BOM.
