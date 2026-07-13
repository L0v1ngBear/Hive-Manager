# TENANT_001 Reset Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a guarded local script that can reset `TENANT_001` business test data while preserving access, global permissions, and active built-in role permissions, without executing the reset in this task.

**Architecture:** A static Node contract test defines safety requirements. A fixed-target MySQL procedure performs transactional database reset, while a Bash wrapper owns preview, confirmation, backup verification, Redis and upload boundaries, and post-reset verification.

**Tech Stack:** Bash, MySQL 8 stored procedures, Docker Compose, Redis CLI, Node.js test runner.

## Global Constraints

- Never connect to the server or run preview/destructive reset commands during implementation.
- Never add the reset SQL to `migration_manifest.txt`, restart, startup, or release scripts.
- The fixed target is `TENANT_001`; all other tenants are forbidden.
- Preserve active built-in `ADMIN` users, at least 20 active built-in roles, their role-permission bindings, global permissions, and the tenant row.
- Delete personal permission overrides, custom roles, non-admin users, all business rows, Redis keys, and upload directories for `TENANT_001`.
- Destructive mode requires `CONFIRM_RESET_TENANT_001=YES` and a verified backup.

---

### Task 1: Define The Static Safety Contract

**Files:**
- Create: `D:/HiveManager/management-ui/tests/deploy-tenant-001-reset.test.js`

**Interfaces:**
- Consumes: canonical desktop deployment package.
- Produces: failing assertions for wrapper, SQL, health checks, and manifest exclusion.

- [ ] Assert fixed target, confirmation capture before `.env`, read-only preview, no container startup, backup-before-SQL ordering, post-reset verification, exact Redis segment and upload path boundaries.
- [ ] Assert SQL uses preserved-admin and preserved-system-role temporary sets, requires administrators and at least 20 built-in roles, physically removes custom roles and personal overrides, dynamically covers tenant tables, and cannot delete `tenant` or `sys_permission`.
- [ ] Run the focused test and verify RED because reset artifacts do not exist.

### Task 2: Implement Transactional Database Reset

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/db-migrations/manual/V20260712_001_reset_tenant_001_business_data.sql`

**Interfaces:**
- Consumes: a MySQL session connected to the Hive business schema.
- Produces: idempotent reset with preserved administrator and built-in-role sets.

- [ ] Create temporary tables for preserved administrator IDs, department names, position names, and active system role IDs.
- [ ] Refuse execution when no active administrator exists or fewer than 20 active system roles exist.
- [ ] Reject affected non-InnoDB tables, start one transaction, disable and restore foreign-key checks, and resignal failures.
- [ ] Delete custom-role permissions, dynamically clear ordinary tenant tables, narrowly reset identity/role tables, and retain only administrator employee support rows.
- [ ] Reset administrator supervisor references that point to removed users, commit, and drop the manual procedure.

### Task 3: Implement Guarded Wrapper

**Files:**
- Create: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/scripts/manual-reset-tenant-001.sh`

**Interfaces:**
- Consumes: `.env`, running MySQL and Redis services, backup scripts, Task 2 SQL, and local uploads root.
- Produces: read-only preview by default and explicitly confirmed reset capability.

- [ ] Capture confirmation before sourcing `.env`, validate the database name and fixed target, and require existing running services without starting containers.
- [ ] Count removable ordinary and special-table rows, preserved administrators, system roles, exact Redis keys, and exact upload directories.
- [ ] Exit after preview unless confirmation is exactly `YES`.
- [ ] Run and verify a fresh backup before SQL, then verify removable residue is zero.
- [ ] Delete only exact-segment Redis keys and exact resolved `TENANT_001` upload directories, then verify zero residue.

### Task 4: Wire Local Package Verification

**Files:**
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/scripts/check-deploy-health.sh`
- Test: `D:/HiveManager/management-ui/tests/deploy-tenant-001-reset.test.js`

- [ ] Require both reset artifacts and verify the confirmation, preview, backup, and preservation markers.
- [ ] Keep both files outside the automatic migration manifest.
- [ ] Run the focused test and require GREEN.
- [ ] Run Bash syntax checks, release-integrity verification, and all management Node tests without running the reset wrapper.
