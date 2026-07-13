# TENANT_001 Business Data Reset Design

Date: 2026-07-12

## Objective

Provide a manual, guarded workflow that resets `TENANT_001` for commercial use without executing it as part of this task. The reset removes test employees and all tenant business data while preserving the tenant, usable administrator access, the global permission catalog, and active built-in role permissions.

## Preserved Data

- The `TENANT_001` row, subscription state, package limits, and license configuration.
- Active `TENANT_001` users assigned to the active built-in `ADMIN` role, so the organization remains accessible after reset.
- Employee-extension, department, and position rows required by those preserved administrators.
- The 20 active built-in roles where `is_system = 1`.
- Role-permission bindings owned by those built-in roles.
- The global `sys_permission` catalog.
- `schema_migration_history`, scheduler metadata, database structure, and every other tenant.

## Removed Data

- Every non-administrator `TENANT_001` user and employee extension.
- Every custom role, its role permissions, and all user-role bindings except active administrator bindings.
- Every `TENANT_001` personal permission override.
- All tenant business, workflow, audit, settings, usage, notification, order, inventory, quality, equipment, installation, attendance, document, print, and announcement rows.
- All `TENANT_001` Redis keys and upload directories.
- Administrator supervisor references that point to removed users.

## Database Strategy

A fixed-target manual SQL file discovers all Hive base tables containing `tenant_code`. It clears `TENANT_001` from every table except the explicitly preserved identity and permission tables. Those tables use narrow predicates based on temporary preserved-user and preserved-role sets.

The SQL rejects an unexpected database, an unexpected target, a missing active built-in `ADMIN` role, no administrator users, fewer than 20 active built-in roles, unsafe dynamically discovered identifiers, or affected non-InnoDB tables. Database changes run in one transaction with foreign-key checks restored on success or failure.

The operation is manual and must never be added to `migration_manifest.txt`, startup, restart, or release scripts.

## Wrapper Safety

- Target is hard-coded as `TENANT_001`.
- Confirmation is captured before `.env` is sourced.
- Default execution is read-only preview and reports removable rows, preserved administrators, built-in roles, Redis keys, and upload directories.
- Destructive mode requires `CONFIRM_RESET_TENANT_001=YES`.
- A fresh database backup and backup verification must complete before SQL execution.
- Redis deletion requires `TENANT_001` as an exact colon-delimited key segment.
- Upload deletion is restricted to directories named exactly `TENANT_001` below the resolved deployment upload root.
- Post-reset verification fails unless every removable database row, cache key, and upload directory is gone.

## Delivery Constraint

This task only creates and validates the local reset workflow in `C:/Users/HUAWEI/Desktop/hive部署_全新配置`. It must not upload the reset files to the server and must not run preview or destructive reset commands against any database.

## Verification

Static tests must prove the fixed target, preview gate, backup ordering, preservation predicates, dynamic tenant-table coverage, transactional failure handling, cache and path boundaries, manifest exclusion, and the explicit prohibition on deleting the tenant row, global permissions, active built-in roles, or preserved administrators.
