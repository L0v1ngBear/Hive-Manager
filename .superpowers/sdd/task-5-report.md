# Task 5 Report: Browsable Role Permissions

## Status

Implemented and committed on `codex/global-layout-repair`.

- Starting commit: `d1d2095`
- Implementation commit: `6cf24b1` (`fix: make role permissions browsable`)

## RED Evidence

Created `management-ui/tests/permission-presentation.test.js` before production code. It defines the required contracts for searchable grouped leaves, selected-only filtering, and unique numeric group leaf IDs.

Command run from `management-ui`:

```text
node --test tests/permission-presentation.test.js
```

Result: exit 1, as expected. Node reported `ERR_MODULE_NOT_FOUND` for `src/views/function/role/permissionPresentation.js`, proving the new pure-function contract was red because its implementation did not exist.

## Implementation

- Added `management-ui/src/views/function/role/permissionPresentation.js`.
  - `permissionGroups(tree, keyword, selectedOnly, selectedIds)` flattens each business group to leaf permissions, filters by permission name/code and selected IDs, and emits numeric IDs.
  - `groupLeafIds(group)` returns unique numeric leaf IDs.
- Replaced only the role permission drawer's collapsed `el-tree-select` with a visible, grouped checkbox list in `management-ui/src/views/function/role/permissionDrawer.vue`.
  - Added search (`aria-label="搜索权限"`), selected-only switch, selected count, per-group select/cancel action, and a bounded scrollable group list.
  - Added `id="role-permission-title"` to the visible drawer heading and `aria-labelledby="role-permission-title"` to the drawer.
  - Tightened drawer padding and checkbox wrapping for compact widths.
- Added `management-ui/tests/permission-presentation.test.js`.

## GREEN Evidence

Command run from `management-ui`:

```text
node --test tests/permission-presentation.test.js tests/permission-ui-hardening.test.js tests/element-plus-announcement-role.test.js
```

Result: exit 0. All 17 tests passed, including the new presentation tests, existing authorization/load-state tests, and stale role-response tests.

Command run from `management-ui`:

```text
npm run build
```

Result: exit 0. Vite built the production client successfully (`1861 modules transformed`, `built in 9.38s`). The informational plugin-timings notice did not report a build failure.

Additional verification:

```text
git diff --cached --check
```

Result: exit 0 before commit.

## Self-Review

- `createRolePermissionLoader`, `syncCommittedPermissionIds`, and the loader's stale-response protection remain in place and were covered by the existing passing hardening tests.
- Forbidden, failed, empty, loading, and ready rendering branches are unchanged.
- The `updateRolePermissions` request shape and save success/failure handling are unchanged.
- Checkbox labels and group actions retain numeric permission IDs; no permission codes or backend contracts changed.
- Employee permission behavior and the role-creation selector were not modified.
- No existing source-contract test asserted an `el-tree-select` in the role permission drawer, so no intentionally superseded source-contract expectation required modification. Existing hardening assertions remain and passed.
- `.superpowers/sdd/progress.md` and `docs/audits/` were not touched. Their pre-existing worktree changes remain unstaged.

## Concerns

No functional concerns identified from the focused test suite or production build. The build emitted only an informational plugin-timings notice.

## Review Fix Evidence

Task 5 review fixes were applied on top of original implementation commit `716f394`. The earlier `6cf24b1` hash recorded above was superseded when the report was included in that amended implementation commit.

### Findings Addressed

- Added `groupActionIds(tree, groupId)` so group selection state and group toggle actions always resolve all leaves from the original permission tree. Search and selected-only filters now affect display only.
- Replaced the one-column checkbox grid with a horizontal flex row. The checkbox input remains fixed at the start while the label takes the remaining width and wraps.
- Strengthened `groupLeafIds` coverage with duplicate string and numeric forms of the same IDs.
- Added a drawer source contract proving the full-tree action helper is used and the checkbox row remains horizontally aligned.

### RED Evidence

Command run from `management-ui`:

```text
node --test tests/permission-presentation.test.js
```

Result: exit 1. Node reported that `permissionPresentation.js` did not export the required `groupActionIds` helper.

Command run from `management-ui`:

```text
node --test tests/element-plus-announcement-role.test.js
```

Result: exit 1 with 13 passing tests and 1 failing test. The new drawer contract could not find full-tree `groupActionIds(...)` integration and the existing checkbox rule still used one-column grid layout.

### GREEN Evidence

Command run from `management-ui`:

```text
node --test tests/permission-presentation.test.js tests/permission-ui-hardening.test.js tests/element-plus-announcement-role.test.js
```

Result: exit 0. All 19 tests passed, including the search-independent group action regression, duplicate/string numeric ID coverage, drawer layout contract, stale-response protection, and forbidden/failed/empty state hardening.

Command run from `management-ui`:

```text
npm run build
```

Result: exit 0. Vite transformed 1861 modules and completed the production build in 8.59 seconds. The plugin-timings message was informational only.

### Fix Self-Review

- Group actions use the original tree and numeric IDs even when the rendered group is narrowed by search or selected-only filtering.
- Search and selected-only presentation behavior is unchanged.
- The save request shape, loader behavior, stale-response guard, permission codes, employee permission UI, and backend contracts remain unchanged.
- `.superpowers/sdd/progress.md` and `docs/audits/` remain untouched and unstaged.
