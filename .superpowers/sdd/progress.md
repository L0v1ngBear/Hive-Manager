# Unified Backend SDD Progress

Branch: codex/unify-hive-backend
Plan: docs/superpowers/plans/2026-07-14-unify-hive-backend.md
Baseline before Task 1: 6b9f9c611f18dc58f646d626b7f4eee27da6cede

Task 1: complete (commits 6b9f9c6..9f0ff01, spec PASS, quality PASS WITH CONCERNS)
- Task 2 mandatory follow-up: replace/augment regex Bean and mapping scans with Spring ApplicationContext and RequestMappingHandlerMapping assertions; cover aliases, composed stereotypes, path/value arrays, constants, and method-specific RequestMapping metadata through runtime resolution.
Task 2: complete (commits 9f0ff01..b0f42b7, final review COMPLIANT/APPROVED, 16 tests passed)
Task 3: complete (commits b0f42b7..8184891, final review PASS/PASS, 40 tests passed)
Task 4: complete (commits 8184891..5d11db8, final review PASS/PASS, 46 tests passed)
- Final review note: add direct security-regression coverage for exact auth allowlist and WeChat malformed-json/errcode/interruption branches if later auth code changes.
Task 4: implementation complete pending commit (focused auth 3 tests passed; auth/permission/context suite 40 tests passed)
Task 5: complete (focused order/approval 36 tests passed; runtime uniqueness/mapping 3 tests passed)
Task 5 final review retry: complete (commits 5d11db8..c17104e, final review PASS/PASS, focused order/approval/runtime suite 41 tests passed)
Task 6: complete (focused inventory/quality/installation suite 10 tests passed; final inventory/quality/installation/permission/runtime suite 47 tests passed)
Task 6 review follow-up: fixed controller text-encoding artifacts after file-level review flagged stale incremental compilation risk; re-ran clean focused inventory/quality/installation/permission/runtime suite with 448 main sources recompiled and 47 tests passed (commit f492695).
Task 6 final review blocker: fixed quality tenant isolation, update permission enforcement, and legacy quality approval aliases; clean inventory/quality/installation/approval/permission/runtime suite passed with 80 tests.
Task 7: complete (customer/document/equipment/label/print packages converged; clean focused customer/document/equipment/label/print/runtime suite passed with 15 tests)
Task 8: complete (commit 944d4b1; notification/attendance packages converged; SMS and WeChat adapters unified; six unique XXL-JOB handlers and one Rabbit listener enforced; focused notification/attendance/WeChat/scheduler/runtime suite passed with 19 tests)
Task 9: complete (commit 5e7c755; `my.management` and `my.hive_back` production/test roots absent; default `my.hive` application scan and canonical mapper scan only; auth audit restored with credential arguments suppressed; source/cardinality guard passed; full clean backend suite passed with 184 tests)
Task 10: complete (commit 64492bf; approved `db-migrations` tree imported byte-for-byte; 74 manifest files match 74 full SHA-256 entries; one top-level `scripts/migrate-db.sh` with required safety helpers; 28 retired application resource SQL files deleted; migration Node gate 2/2, schema-only baseline gate, and clean backend suite 184/184 passed)
Task 11: complete (commit 3bcf097; management UI base/proxy switched from `/web` to `/api`; admin authentication namespaced under `/auth/admin`; order, approval, quality, and installation request modules use canonical routes; focused route tests 4/4, full Node suite 51/51, and Vite production build passed)
Task 12: complete except release-host Docker validation (commit bf1aa0e; `deploy` single-service source created; one `hive-backend`, one `/api` upstream, unified variables and operational scripts; topology 3/3, combined route/topology 5/5, YAML uniqueness/structure and 131/131 app-variable mapping passed; all Bash syntax passed; local Docker CLI unavailable)
Task 13: complete (commit 0077f10; build/instance identity headers and public `/api/health`; clean package 186/186; direct Java 21 startup and 10-route same-build/same-process smoke passed)
Task 14: package synchronized (release commit ca9fccf; final backend SHA-256 95d7c03998700a9d9ae494de017442269d0b1123c7e4ec904f05bb16d9fff33c; clean source UI tree f7794088fd9c19fe38330d104274c04078e4b3d2ace194f542da593851e3236d; desktop package has one JAR, 73 UI files, 74 migrations, one backend service, no retired runtime references; external application/docs/.env rollback snapshots verified; static health, release integrity, topology, schema baseline and clean index UI suite passed; release-host Docker gate remains open because local Docker CLI is unavailable)
