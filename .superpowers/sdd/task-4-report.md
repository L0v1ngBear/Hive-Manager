# Task 4 Report: Organization And Equipment Pages

## TDD Record

### RED

Command:

```powershell
node --test tests/element-plus-organization-equipment.test.js
```

Result: failed as expected with 0 passing and 2 failing assertions. The organization assertion reported a missing `<el-drawer>` tag and the equipment assertion reported a missing `<el-table>` tag.

Additional payload-boundary RED command:

```powershell
node --test tests/element-plus-organization-equipment.test.js
```

Result: failed as expected with 2 passing and 1 failing assertion because the organization parent selector did not explicitly clear to `''`.

### GREEN And Regression

Commands:

```powershell
node --test tests/element-plus-organization-equipment.test.js
npx eslint src/views/function/organization/organization.vue src/views/function/equipment/equipment.vue
npm run build
```

Results:

- Target test: 3 passing, 0 failing.
- Target ESLint: exit 0 with no findings.
- Production build: exit 0; Vite transformed 1843 modules and completed successfully.

## Changes

- Migrated organization commands and department editor to explicitly imported Element Plus drawer, form, input, select, input-number, switch, button, tag, and empty-state controls while retaining the recursive `DepartmentNode` tree, API calls, event names, and numeric/null payload normalization.
- Preserved `sortNo = 0` and ensured a cleared parent selector serializes through the existing root-department (`null`) path.
- Migrated equipment filters, table, pagination, editor, detail panel, tags, loading, and empty states to explicitly imported Element Plus controls.
- Preserved current-page table export through the Element Plus table DOM, detail-then-record request sequence, the 20-record inspection limit, existing API calls, and row actions.
- Added the Task 4 contract test and updated organization/equipment module migration status documentation.

## Commit

- Implementation commit: `a3689e39dccb1968521b2af882f0a29d73464d25` (`feat: migrate organization and equipment controls`)

## Concerns

- Existing organization member-request race and stale-member error-state risks remain unchanged by scope.
- Existing command-level permission visibility remains unchanged; the backend still enforces save/delete/disable permissions.
- The Element Plus table export relies on the rendered current-page DOM, preserving the prior export scope; it should be manually checked with representative data before release.

## Review Fix: Equipment Empty State

### Reviewer Finding

Verified against `equipment.vue`: `ElTable` retained its built-in empty rendering while an additional `ElEmpty` was rendered after the table when `devices.length === 0`, producing two empty states for one list.

### RED

Command:

```powershell
node --test tests/element-plus-organization-equipment.test.js
```

Result: failed as expected with 3 passing and 1 failing test. The new contract reported that the equipment table had no `#empty` slot containing `ElEmpty`.

### Fix

- Added a source contract requiring exactly one `ElEmpty` inside the equipment `ElTable` `#empty` slot and prohibiting another empty node between the table and pagination.
- Moved the existing equipment empty state into `ElTable`'s `#empty` slot and removed the duplicate external conditional rendering.
- Loading, permission visibility, pagination, export, row actions, and API logic were unchanged.

### GREEN And Regression

Commands:

```powershell
node --test tests/element-plus-organization-equipment.test.js
npx eslint src/views/function/organization/organization.vue src/views/function/equipment/equipment.vue
npm run build
```

Results:

- Target test: 4 passing, 0 failing.
- Target ESLint: exit 0 with no findings.
- Production build: exit 0; Vite transformed 1843 modules and completed successfully.

## Second Review Fix: Persistent Failures And Request Safety

### Review And Baseline Verification

- Verified that equipment list failures left the prior `devices` rows visible because `fetchDevices` only replaced rows after a successful response and had no persistent failure state.
- Verified that organization member selection kept prior members while loading, accepted every response, and allowed an older request's response or `finally` to overwrite a newer selection.
- Verified against baseline commit `2355d9c` that the disabled equipment code previously explained: `设备码已用于固定二维码，创建后不可修改。`
- Verified that equipment pagination did not bind its disabled state to `loading`.
- Verified the project request error shapes before implementing classification: business failures reject the response envelope with `error.code`; HTTP failures retain `error.response.status` and `error.response.data.code`; network failures may expose a non-numeric Axios `error.code`. The local classifiers also accept `error.statusCode` for compatible callers.

### RED

Command:

```powershell
node --test tests/element-plus-organization-equipment.test.js
```

Result: failed as expected with 4 passing and 3 failing tests. Failures identified missing equipment row clearing/failure state, missing organization member clearing/request sequencing, and missing locked-code explanation/loading-disabled pagination.

### Fix

- Equipment now clears rows and pagination totals before each list request, persists separate unauthorized, forbidden, server, and network failure presentations, and offers a retry action. Failure, loading, true-empty, and permission states are mutually exclusive.
- Organization now clears members immediately on selection, uses a monotonically increasing request ID for last-request-wins behavior, protects success/error/finally writes from stale requests, persists distinct unauthorized/forbidden/request failures, and offers member retry.
- Restored the baseline equipment-code lock explanation through an explicitly imported `ElTooltip` while retaining the disabled input.
- Equipment pagination now binds `disabled` to `loading`; API calls, page parameters, route permissions, command visibility, and value types remain unchanged.

### GREEN And Verification

Commands:

```powershell
node --test tests/element-plus-organization-equipment.test.js
npx eslint src/views/function/organization/organization.vue src/views/function/equipment/equipment.vue
npm run build
```

Results:

- Target test: 7 passing, 0 failing.
- Target ESLint: exit 0 with no findings.
- Production build: exit 0; Vite transformed 1843 modules and completed successfully.

### Remaining Concerns

- The earlier report concern about organization member-request races and stale-member failures is resolved by this review fix.
- Equipment detail/inspection requests retain their existing sequence and are outside this list/member-state review scope.
- Command-level permission visibility remains unchanged; backend permission enforcement is still authoritative.
