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
