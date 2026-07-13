# Management UI Element Plus Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every suitable native management control with real Element Plus components while preserving Hive business contracts and protected domain surfaces.

**Architecture:** Establish a small explicit Element Plus foundation first, then migrate disjoint page groups using local component imports and module-specific source-contract tests. Keep API modules, payload types, permission logic, responsive data behavior, print DOM, label rendering, and specialized visualization unchanged; update each module record as its migration finishes.

**Tech Stack:** Vue 3.5, Vite 8, Element Plus 2.13, Tailwind CSS 4, Pinia, Node test runner, ESLint, Playwright/in-app browser visual QA.

## Global Constraints

- Use Element Plus 2.13 already present in `management-ui/package.json`; add no UI framework.
- Import components explicitly from `element-plus`; do not call `app.use(ElementPlus)`.
- Register only `ElLoadingDirective` globally for existing and migrated `v-loading` usage.
- Preserve every API path, HTTP method, payload value type, response shape, permission code, feature flag, route, emitted event, and business state transition.
- Preserve disabled permission presentation and disabled-reason tooltips.
- Preserve custom print DOM/CSS, barcode and QR rendering, label preview/canvas, organization chart visualization, dynamic-column export, attachment upload contract, and business-time correction semantics.
- Distinguish loading, true empty, permission denied, and request failure states.
- Keep Element Plus date controls on the exact existing API string format through explicit `value-format`.
- Keep numeric select and radio values numeric where the current API uses numbers.
- Update the corresponding file under `docs/management-ui/modules/` in the same task.
- Use test-first development: every task begins with a failing source-contract test and records the expected RED failure before production edits.

---

### Task 1: Element Plus Foundation And Theme Bridge

**Files:**

- Create: `management-ui/src/plugins/elementPlus.js`
- Modify: `management-ui/src/main.js`
- Modify: `management-ui/src/style.css`
- Modify: `docs/management-ui/modules/shared-foundation.md`
- Test: `management-ui/tests/element-plus-foundation.test.js`

**Interfaces:**

- Produces: `installElementPlusFoundation(app)` which registers `loading` with `ElLoadingDirective`.
- Produces: stable semantic variables `--ys-control-height`, `--ys-control-radius`, `--ys-focus-ring`, and their `--el-*` mappings.
- Consumes: the existing Vue application instance in `main.js` and existing Hive color tokens.

- [ ] **Step 1: Write the failing foundation test**

```js
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const main = readFileSync(new URL("../src/main.js", import.meta.url), "utf8");
const plugin = readFileSync(
  new URL("../src/plugins/elementPlus.js", import.meta.url),
  "utf8",
);
const style = readFileSync(
  new URL("../src/style.css", import.meta.url),
  "utf8",
);

test("registers only the required Element Plus loading directive", () => {
  assert.match(main, /installElementPlusFoundation\(app\)/);
  assert.match(plugin, /ElLoadingDirective/);
  assert.match(
    plugin,
    /app\.directive\(['"]loading['"],\s*ElLoadingDirective\)/,
  );
  assert.doesNotMatch(main, /app\.use\(ElementPlus\)/);
});

test("maps Hive semantic control tokens into Element Plus variables", () => {
  for (const token of [
    "--ys-control-height",
    "--ys-control-radius",
    "--ys-focus-ring",
  ]) {
    assert.match(style, new RegExp(token));
  }
  assert.match(style, /--el-component-size:/);
  assert.match(style, /--el-border-radius-base:/);
});
```

- [ ] **Step 2: Run the foundation test and verify RED**

Run: `node --test tests/element-plus-foundation.test.js`

Expected: FAIL because `src/plugins/elementPlus.js` and the semantic tokens do not exist.

- [ ] **Step 3: Implement the explicit foundation installer**

```js
// src/plugins/elementPlus.js
import { ElLoadingDirective } from "element-plus";

export function installElementPlusFoundation(app) {
  app.directive("loading", ElLoadingDirective);
}
```

In `main.js`, import the function, then call `installElementPlusFoundation(app)` after router/Pinia setup and before `app.mount('#app')`.

In `style.css`, set the semantic control values on `body` and map them without replacing existing brand colors:

```css
body {
  --ys-control-height: 40px;
  --ys-control-radius: 8px;
  --ys-focus-ring: 0 0 0 3px rgba(31, 63, 95, 0.18);
  --el-component-size: var(--ys-control-height);
  --el-border-radius-base: var(--ys-control-radius);
}

.el-input__wrapper:focus-within,
.el-select__wrapper.is-focused,
.el-textarea__inner:focus {
  box-shadow: var(--ys-focus-ring) !important;
}
```

- [ ] **Step 4: Verify foundation and full baseline**

Run: `node --test tests/element-plus-foundation.test.js`

Expected: 2 tests pass.

Run: `node --test tests/*.test.js`

Expected: all existing tests pass.

- [ ] **Step 5: Update foundation documentation and commit**

Change the migration status in `shared-foundation.md` to `Foundation migrated`, list the registered directive and final token names, then commit:

```bash
git add management-ui/src/plugins/elementPlus.js management-ui/src/main.js management-ui/src/style.css management-ui/tests/element-plus-foundation.test.js docs/management-ui/modules/shared-foundation.md
git commit -m "feat: establish Element Plus UI foundation"
```

### Task 2: Announcement And Role Pages

**Files:**

- Modify: `management-ui/src/views/function/announcement/announcement.vue`
- Modify: `management-ui/src/views/function/announcement/publish.vue`
- Modify: `management-ui/src/views/function/role/role.vue`
- Modify: `management-ui/src/views/function/role/createRoleDrawer.vue`
- Modify: `management-ui/src/views/function/role/permissionDrawer.vue`
- Modify: `docs/management-ui/modules/announcement.md`
- Modify: `docs/management-ui/modules/role.md`
- Test: `management-ui/tests/element-plus-announcement-role.test.js`

**Interfaces:**

- Consumes: `getAnnouncements`, `publishAnnouncement`, and role API functions unchanged.
- Produces: Element Plus list/form/table/pagination commands while preserving permission directives and drawer events.

- [ ] **Step 1: Write the failing page contract test**

```js
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

test("announcement pages use Element Plus controls", () => {
  const list = read("../src/views/function/announcement/announcement.vue");
  const publish = read("../src/views/function/announcement/publish.vue");
  for (const tag of ["el-button", "el-empty"])
    assert.match(list, new RegExp(`<${tag}\\b`));
  for (const tag of ["el-form", "el-input", "el-select", "el-button"]) {
    assert.match(publish, new RegExp(`<${tag}\\b`));
  }
  assert.doesNotMatch(publish, /<(input|select|textarea|button)\b/);
});

test("role list and creation form use Element Plus controls", () => {
  const list = read("../src/views/function/role/role.vue");
  const create = read("../src/views/function/role/createRoleDrawer.vue");
  for (const tag of ["el-table", "el-table-column", "el-button", "el-empty"]) {
    assert.match(list, new RegExp(`<${tag}\\b`));
  }
  for (const tag of ["el-drawer", "el-form", "el-input", "el-tree-select"]) {
    assert.match(create, new RegExp(`<${tag}\\b`));
  }
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test tests/element-plus-announcement-role.test.js`

Expected: FAIL on missing Element Plus list/form/table components.

- [ ] **Step 3: Replace standard controls**

Use `ElButton`, `ElEmpty`, `ElTag`, and `ElSkeleton` on the announcement list while preserving the existing announcement card and recipient layout. Use `ElForm`, `ElFormItem`, `ElSelect`, `ElOption`, `ElInput`, and `ElButton` on publish; keep `level`, trimmed title/content, `maxlength`, `publishing`, and navigation behavior unchanged.

Use `ElTable`, `ElTableColumn`, `ElButton`, `ElEmpty`, and `ElPagination` for the role list. Keep `v-permission`, role IDs, system-role display, permission-tree values, `success`, and `close` events unchanged. Keep all Element components explicitly imported in each `<script setup>`.

- [ ] **Step 4: Verify and document**

Run: `node --test tests/element-plus-announcement-role.test.js`

Run: `npx eslint src/views/function/announcement/announcement.vue src/views/function/announcement/publish.vue src/views/function/role/role.vue src/views/function/role/createRoleDrawer.vue src/views/function/role/permissionDrawer.vue`

Expected: tests and ESLint pass.

Mark both module files as `Element Plus migrated`; record the components and remaining permission risks.

- [ ] **Step 5: Commit**

```bash
git add management-ui/src/views/function/announcement management-ui/src/views/function/role management-ui/tests/element-plus-announcement-role.test.js docs/management-ui/modules/announcement.md docs/management-ui/modules/role.md
git commit -m "feat: migrate announcement and role controls"
```

### Task 3: Customer And Document Pages

**Files:**

- Modify: `management-ui/src/views/function/customer/customer.vue`
- Modify: `management-ui/src/views/function/customer/customerCreate.vue`
- Modify: `management-ui/src/views/function/document/document.vue`
- Modify: `management-ui/src/components/DragAttachmentUpload.vue`
- Modify: `docs/management-ui/modules/customer.md`
- Modify: `docs/management-ui/modules/document.md`
- Test: `management-ui/tests/element-plus-customer-document.test.js`

**Interfaces:**

- Consumes: customer and document API wrappers unchanged, `DragAttachmentUpload` events `select`, `download`, and `remove` unchanged.
- Produces: Element Plus customer table/dialog/drawer and document filters/table/actions.

- [ ] **Step 1: Write the failing source contract**

```js
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

test("customer surfaces use Element Plus table dialog drawer and form", () => {
  const list = read("../src/views/function/customer/customer.vue");
  const editor = read("../src/views/function/customer/customerCreate.vue");
  for (const tag of [
    "el-input",
    "el-select",
    "el-table",
    "el-pagination",
    "el-dialog",
  ]) {
    assert.match(list, new RegExp(`<${tag}\\b`));
  }
  for (const tag of ["el-drawer", "el-form", "el-input", "el-select"]) {
    assert.match(editor, new RegExp(`<${tag}\\b`));
  }
});

test("document page uses Element Plus filters and data states", () => {
  const source = read("../src/views/function/document/document.vue");
  for (const tag of [
    "el-input",
    "el-select",
    "el-table",
    "el-button",
    "el-empty",
  ]) {
    assert.match(source, new RegExp(`<${tag}\\b`));
  }
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test tests/element-plus-customer-document.test.js`

Expected: FAIL on missing list/form components.

- [ ] **Step 3: Migrate customer controls**

Replace filter inputs/selects, table, pagination, detail overlay, and editor drawer with Element Plus. Render dynamic customer columns using `ElTableColumn` slots and preserve column order keys, row-click behavior, `.stop` action buttons, field configuration, contacts/projects arrays, and `success`/`close` events.

- [ ] **Step 4: Migrate document controls and upload shell**

Use Element Plus for document search/type filter, commands, table, empty state, and folder dialog. Keep the existing multipart upload API. Refactor `DragAttachmentUpload` to use `ElButton` for commands while preserving the native hidden file input and all current event names; validate drag/drop against `accept` in the same way as click selection.

- [ ] **Step 5: Verify, document, and commit**

Run: `node --test tests/element-plus-customer-document.test.js`

Run: `npx eslint src/views/function/customer/customer.vue src/views/function/customer/customerCreate.vue src/views/function/document/document.vue src/components/DragAttachmentUpload.vue`

Expected: pass.

Update customer/document module status and commit:

```bash
git add management-ui/src/views/function/customer management-ui/src/views/function/document management-ui/src/components/DragAttachmentUpload.vue management-ui/tests/element-plus-customer-document.test.js docs/management-ui/modules/customer.md docs/management-ui/modules/document.md
git commit -m "feat: migrate customer and document controls"
```

### Task 4: Organization And Equipment Pages

**Files:**

- Modify: `management-ui/src/views/function/organization/organization.vue`
- Modify: `management-ui/src/views/function/equipment/equipment.vue`
- Modify: `docs/management-ui/modules/organization.md`
- Modify: `docs/management-ui/modules/equipment.md`
- Test: `management-ui/tests/element-plus-organization-equipment.test.js`

**Interfaces:**

- Consumes: organization/equipment API wrappers and existing permission behavior unchanged.
- Produces: Element Plus department/member panels and equipment list/editor/detail surfaces.

- [ ] **Step 1: Write the failing contract**

```js
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

test("organization keeps its domain tree and migrates standard editor controls", () => {
  const source = read("../src/views/function/organization/organization.vue");
  for (const tag of [
    "el-drawer",
    "el-form",
    "el-input",
    "el-select",
    "el-input-number",
  ]) {
    assert.match(source, new RegExp(`<${tag}\\b`));
  }
  assert.match(source, /renderDepartmentNode/);
});

test("equipment uses Element Plus table pagination and drawers", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  for (const tag of [
    "el-table",
    "el-pagination",
    "el-drawer",
    "el-form",
    "el-input-number",
  ]) {
    assert.match(source, new RegExp(`<${tag}\\b`));
  }
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test tests/element-plus-organization-equipment.test.js`

Expected: FAIL on missing drawers/forms/tables.

- [ ] **Step 3: Implement organization migration**

Keep the recursive domain tree and member selection logic. Replace department editor and standard commands with `ElDrawer`, `ElForm`, `ElInput`, `ElSelect`, `ElInputNumber`, `ElSwitch`, `ElButton`, and `ElEmpty`. Preserve selected department IDs, save/delete payloads, and `sortNo = 0` as a valid value.

- [ ] **Step 4: Implement equipment migration**

Use Element Plus filters, table, pagination, editor drawer, detail drawer, tags, form controls, loading, and empty states. Preserve current export scope, detail request sequence, inspection record limit, API calls, and row action behavior.

- [ ] **Step 5: Verify, document, and commit**

Run targeted test and ESLint for both pages. Update module statuses, then commit all four source/doc/test files with message `feat: migrate organization and equipment controls`.

### Task 5: Employee And Attendance Pages

**Files:**

- Modify: `management-ui/src/views/function/employee/employee.vue`
- Modify: `management-ui/src/views/function/employee/employeeCreate.vue`
- Modify: `management-ui/src/views/function/employee/EmployeePermissionDrawer.vue`
- Modify: `management-ui/src/views/function/attendance/attendanceManagement.vue`
- Modify: `docs/management-ui/modules/employee.md`
- Modify: `docs/management-ui/modules/attendance.md`
- Test: `management-ui/tests/element-plus-employee-attendance.test.js`

**Interfaces:**

- Preserves: boss-root organization chart helper, user permission override values, employee import/export, attendance query/export/rule API contracts.

- [ ] **Step 1: Write and run a RED contract test**

Create a Node test that asserts employee list/editor contain `el-table`, `el-pagination`, `el-drawer`, `el-form`, `el-input`, `el-select`, `el-date-picker`, `el-radio-group`, and that attendance contains `el-table`, `el-pagination`, `el-date-picker`, `el-time-picker`, and `el-input-number`. It must also assert `employee.vue` still imports `buildEmployeeOrganizationChart` and `EmployeePermissionDrawer.vue` still uses `el-tree-select`.

Run: `node --test tests/element-plus-employee-attendance.test.js`

Expected: FAIL on components not yet migrated.

- [ ] **Step 2: Migrate employee list and editor**

Replace standard filters, table, pagination, details/editor drawer, form fields, date/radio controls, commands, empty, and loading states. Keep dynamic tenant fields, local column order, organization chart, import file input contract, leader search, permission directives, and editor events unchanged.

- [ ] **Step 3: Migrate attendance list and rule drawer**

Replace filters, dates, table, pagination, rule drawer controls, time fields, number fields, commands, and empty/loading states. Keep query parameter formats, Blob export, `attendance:record:list`/`attendance:*` behavior, and overnight rule values unchanged.

- [ ] **Step 4: Verify and commit**

Run the targeted test, existing organization-root test, permission hardening test, and targeted ESLint. Update both module documents and commit with `feat: migrate employee and attendance controls`.

### Task 6: Tenant And Price Pages

**Files:**

- Modify: `management-ui/src/views/function/tenant/tenant.vue`
- Modify: `management-ui/src/views/function/price/price.vue`
- Modify: `management-ui/src/views/function/price/priceCreate.vue`
- Modify: `docs/management-ui/modules/tenant.md`
- Modify: `docs/management-ui/modules/price.md`
- Test: `management-ui/tests/element-plus-tenant-price.test.js`

**Interfaces:**

- Preserves platform-only tenant APIs and price matrix payloads, dates, decimals, imports, and exports.

- [ ] **Step 1: Write and run the RED test**

Assert tenant uses `el-table`, `el-drawer`, `el-form`, `el-upload`, `el-input-number`, `el-date-picker`, `el-checkbox-group`, and `el-switch`. Assert price list/editor use `el-table`, `el-pagination`, `el-drawer`, `el-form`, `el-input-number`, `el-date-picker`, and `el-select`, and retain `value-format="YYYY-MM-DD"`.

Run: `node --test tests/element-plus-tenant-price.test.js`

Expected: FAIL before migration.

- [ ] **Step 2: Migrate tenant surfaces**

Replace list, three drawers, forms, logo upload shell, feature controls, capacity/date fields, switches, commands, loading, and empty states. Preserve API calls, platform guard, JSON text behavior, separate logo transaction, status confirmation, and numeric capacity payloads.

- [ ] **Step 3: Migrate price surfaces**

Replace filters, table, pagination, editor/detail drawers, dates, decimal fields, customer/model selects, import command shell, and empty/loading states. Preserve local-date defaults by replacing UTC `toISOString()` with a local `YYYY-MM-DD` formatter covered by the test; keep price payload precision and current endpoints.

- [ ] **Step 4: Verify and commit**

Run targeted test and ESLint. Update docs, then commit `feat: migrate tenant and price controls`.

### Task 7: Installation And Quality Pages

**Files:**

- Modify: `management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: `management-ui/src/views/function/badProduct/badProduct.vue`
- Modify: `docs/management-ui/modules/installation-task.md`
- Modify: `docs/management-ui/modules/quality.md`
- Test: `management-ui/tests/element-plus-installation-quality.test.js`

**Interfaces:**

- Preserves installation status requirements, logistics fields, construction fields, special notes, quality process submission, attachments, and time correction.

- [ ] **Step 1: Write and run RED**

Assert both pages contain `el-table`, `el-pagination`, `el-dialog` or `el-drawer`, `el-form`, `el-input`, `el-select`, `el-tag`, `el-empty`, and loading bindings. Assert both still import `DragAttachmentUpload`; quality must retain `BusinessTimeCorrectionPanel`.

Run: `node --test tests/element-plus-installation-quality.test.js`

Expected: FAIL.

- [ ] **Step 2: Migrate both pages**

Use Element Plus for standard filters, tables, pagination, status tags, form containers, fields, commands, dialogs/drawers, loading and empty states. Preserve installation status validation and logistics/installer/special-note payloads. Preserve quality type/scope, loss bracket payload, process submission, attachment order, permission directives, and time correction.

- [ ] **Step 3: Verify and commit**

Run targeted test plus `installation-task-shipped-logistics.test.js` and `installation-task-special-note.test.js`, targeted ESLint, update docs, and commit `feat: migrate installation and quality controls`.

### Task 8: Approval, Dashboard, Navigation, And Authentication

**Files:**

- Modify: `management-ui/src/views/function/approval/approvalCenter.vue`
- Modify: `management-ui/src/views/dashboard/index.vue`
- Modify: `management-ui/src/layout/components/Navbar.vue`
- Modify: `management-ui/src/layout/components/Sidebar.vue`
- Modify: `management-ui/src/views/Login.vue`
- Modify: `management-ui/src/views/JoinOrganization.vue`
- Modify: `management-ui/src/views/ForcePasswordChange.vue`
- Modify: `management-ui/src/views/NoPermission.vue`
- Modify: `docs/management-ui/modules/approval.md`
- Modify: `docs/management-ui/modules/dashboard.md`
- Modify: `docs/management-ui/modules/layout-navigation.md`
- Modify: `docs/management-ui/modules/authentication.md`
- Test: `management-ui/tests/element-plus-shell-approval.test.js`

**Interfaces:**

- Preserves approval tab permission matrix and API dispatch, dashboard routes, navigation permission behavior, login encryption/memory/scan flows, and auth API payloads.

- [ ] **Step 1: Write and run RED**

Assert approval uses Element Plus tabs/badges/table/pagination/forms/descriptions and retains all five approval type keys. Assert login/join/password pages use Element Plus forms/inputs/buttons. Assert Navbar and Sidebar use Element Plus popover/dropdown/badge/button where appropriate while retaining permission warning calls.

Run: `node --test tests/element-plus-shell-approval.test.js`

Expected: FAIL.

- [ ] **Step 2: Migrate approval and dashboard**

Replace standard tabs, counters, filters, table, pagination, descriptions, forms, dates/numbers, commands, dialogs, tags, loading and empty states. Keep five API families, current permissions, attachment flow, selected auditors, and list mapping unchanged. Convert dashboard commands and announcement empty/loading states without changing metric layout or routes.

- [ ] **Step 3: Migrate navigation and authentication**

Use Element Plus dropdown/popover/badge/button for interactive navigation overlays and commands; retain sidebar structure, search results, notification APIs, feature/permission checks, and responsive shell. Use Element Plus form controls for login/join/password/denied actions while preserving password reset, QR scan, remember-login, organization join, and redirect behavior.

- [ ] **Step 4: Verify and commit**

Run targeted test, `auth-storage-security.test.js`, `permission-ui-hardening.test.js`, targeted ESLint, update four docs, and commit `feat: migrate approval and application shell controls`.

### Task 9: Order Management

**Files:**

- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: `docs/management-ui/modules/order.md`
- Test: `management-ui/tests/element-plus-order.test.js`

**Interfaces:**

- Preserves all order API wrappers, two state flows, state-derived permissions, dynamic columns/export, customer/project suggestions, attachments, time correction, and row action propagation.

- [ ] **Step 1: Write and run RED**

Assert order uses `el-input`, `el-select`, `el-date-picker`, `el-input-number`, `el-table`, `el-pagination`, `el-drawer`, `el-dialog`, `el-form`, `el-tag`, `el-progress`, `el-empty`, and `v-loading`. Assert the source retains `ORDER_STATUS`, `ORDER_STATUS_SEQUENCE`, `order:status:`, `TableColumnSettings`, `DragAttachmentUpload`, and `BusinessTimeCorrectionPanel`.

Run: `node --test tests/element-plus-order.test.js`

Expected: FAIL.

- [ ] **Step 2: Replace order standard controls**

Migrate filters, dates, numbers, list table, pagination, detail/editor drawers, warning settings dialog, tags, progress, forms, commands, loading, and empty states. Replace the `dangerouslyUseHTMLString` warning-settings prompt with an owned `ElDialog` form. Preserve every current v-model type, `data-field` focus contract or update `focusFormField` to target Element wrappers, state permission mapping, `.stop`, dynamic column order, export, and API calls.

- [ ] **Step 3: Verify and commit**

Run targeted test, `order-unified.test.js`, `permission-ui-hardening.test.js`, targeted ESLint, update order document, and commit `feat: migrate order management controls`.

### Task 10: Inventory And Inventory Detail

**Files:**

- Modify: `management-ui/src/views/function/inventory/inventory.vue`
- Modify: `management-ui/src/views/function/inventory/InventoryModelDetail.vue`
- Modify: `docs/management-ui/modules/inventory.md`
- Modify: `docs/management-ui/modules/inventory-detail.md`
- Test: `management-ui/tests/element-plus-inventory.test.js`

**Interfaces:**

- Preserves inventory API wrappers, recognition candidate draft selection, dynamic columns, barcode preview, in/out payloads, attachments/time correction, and route query contract.

- [ ] **Step 1: Write and run RED**

Assert both pages use Element Plus filters, number/date controls, drawers, forms, pagination, tags, empty and loading states. Require `el-table` only where dynamic responsive/export behavior is preserved. Assert inventory retains `imageRecognitionCandidates`, `TableColumnSettings`, `DragAttachmentUpload`, and `BusinessTimeCorrectionPanel`.

Run: `node --test tests/element-plus-inventory.test.js`

Expected: FAIL.

- [ ] **Step 2: Migrate inventory surfaces**

Replace standard filters, form fields, number/date inputs, commands, warning settings drawer, in/out drawers, import/recognition dialog shell, pagination, tags, loading and empty states. Keep responsive/export table native if `ElTable` cannot preserve the current dynamic-column and mobile `data-label` contract; document that protected exception instead of forcing replacement.

- [ ] **Step 3: Migrate inventory detail surfaces**

Replace sorting, commands, drawers, forms, number inputs, tags, loading and empty states. Clear stale cloth detail before each request and bind the preview barcode to the submitted barcode without changing the API.

- [ ] **Step 4: Verify and commit**

Run targeted test and ESLint, update both docs with any protected table exception, and commit `feat: migrate inventory controls`.

### Task 11: Receipt And Label Printing

**Files:**

- Modify: `management-ui/src/views/function/receipt.vue`
- Modify: `management-ui/src/views/function/label.vue`
- Modify: `docs/management-ui/modules/receipt-print.md`
- Modify: `docs/management-ui/modules/label.md`
- Test: `management-ui/tests/element-plus-printing.test.js`

**Interfaces:**

- Preserves receipt print HTML/CSS, millimeter layout, page breaks, browser print timing, QR/barcode rendering, label preview, print task reporting, and all APIs.

- [ ] **Step 1: Write and run RED**

Assert receipt uses `el-tabs`, `el-form`, `el-input`, `el-input-number`, `el-checkbox`, `el-select`, and `el-button`, while still containing the native print tables and `window.print`/print-window code. Assert label uses `el-tabs`, `el-badge`, `el-form`, `el-input-number`, `el-switch`, `el-select`, and `el-button`, while retaining `QRCode`, `JsBarcode`, and print-style generation.

Run: `node --test tests/element-plus-printing.test.js`

Expected: FAIL.

- [ ] **Step 2: Migrate peripheral controls only**

Replace tabs, calibration/template forms, numeric fields, switches/checkboxes, selectors, commands, task/target lists, loading and empty states. Do not replace or wrap actual receipt print tables, cloned print HTML, label preview DOM, QR/barcode nodes, millimeter/page-break CSS, or print-result reporting.

- [ ] **Step 3: Verify and commit**

Run targeted test and ESLint. Open receipt/label pages and compare print preview at current supported sizes. Update docs with explicit protected nodes and commit `feat: migrate printing page controls`.

### Task 12: Editable User Manual

**Files:**

- Modify: `management-ui/src/views/manual/UserManual.vue`
- Modify: `docs/management-ui/modules/manual.md`
- Test: `management-ui/tests/element-plus-manual.test.js`

**Interfaces:**

- Preserves custom manual JSON, old-text fallback, full editable content model, local edit state, save API, reset/templates, Markdown export, and section navigation.

- [ ] **Step 1: Write and run RED**

Assert manual uses `el-button`, `el-input`, `el-dialog`, `el-empty`, and `v-loading`, retains `getCustomManual`, `saveCustomManualContent`, `applyRecommendedTemplate`, and Markdown export creation, and no longer uses `window.confirm`.

Run: `node --test tests/element-plus-manual.test.js`

Expected: FAIL.

- [ ] **Step 2: Migrate manual editing commands and forms**

Replace editor commands, text fields, textareas, reset confirmation, dialogs, loading and empty states with Element Plus. Keep the article/hero/card layout, section navigation, editing coverage, custom JSON schema, fallback, and Markdown output unchanged.

- [ ] **Step 3: Verify and commit**

Run targeted test and ESLint, update manual doc, and commit `feat: migrate manual editing controls`.

### Task 13: Whole-Application Verification, Visual QA, Documentation Status, And Packaging

**Files:**

- Modify: `docs/management-ui/README.md`
- Modify: any module document whose final protected exception differs from the plan
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-ui/dist/**` by verified build synchronization
- Modify: `C:/Users/HUAWEI/Desktop/hive部署_全新配置/RELEASE_BUILD_INFO.txt`
- Test: all `management-ui/tests/*.test.js`

**Interfaces:**

- Consumes: all migrated pages and module records.
- Produces: verified production `dist`, updated release hash, and complete migration registry.

- [ ] **Step 1: Run source and quality verification**

```bash
node --test tests/*.test.js
npx eslint src tests
npm run build
```

Expected: all tests pass, ESLint exits 0, Vite production build exits 0.

- [ ] **Step 2: Run responsive visual QA**

Start Vite on a free localhost port. Using the in-app browser, capture and inspect representative pages at 1440x900, 1024x768, and 390x844. Cover at minimum one page from each task plus order, inventory, receipt, label, and manual. Verify no overlaps, clipped controls, blank dialogs/drawers, text overflow, broken focus states, or horizontal page scroll.

- [ ] **Step 3: Exercise state and permission variants**

For representative pages, verify loading, empty, API error, permission-disabled, long text, narrow viewport, dialog/drawer close, keyboard focus, and pagination. Re-run existing permission tests and preserve disabled content opacity/visibility requirements.

- [ ] **Step 4: Complete documentation status**

Change every migrated module from `Audit baseline` to `Element Plus migrated` or `Element Plus migrated with protected custom surface`. Keep unresolved permission/API risks listed. Update `docs/management-ui/README.md` status legend and registry.

- [ ] **Step 5: Synchronize deployment artifact**

Resolve and verify the exact source `D:/HiveManager/management-ui/dist` and target `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-ui/dist`, then mirror the source into the target. Compare relative file names and SHA-256 values exactly. Recompute the release UI hash using lowercase file SHA-256, two spaces, POSIX relative path, LF, and bytewise path sorting; update `ManagementCommit`, `ManagementUiSHA256`, `ManagementUiFileCount`, and `BuiltAt` in `RELEASE_BUILD_INFO.txt`.

- [ ] **Step 6: Run deployment static verification**

```bash
bash scripts/verify-release-integrity.sh
bash scripts/verify-low-cost-mode.sh
bash scripts/secret-scan.sh
bash scripts/check-management-ui-key.sh
```

Expected: every script exits 0. Do not upload to or restart the server unless the user separately requests deployment.

- [ ] **Step 7: Final review and commit**

Run `git diff --check`, confirm only intended files changed, dispatch a whole-branch code review, fix all Critical/Important findings, then commit final documentation/build metadata changes with:

```bash
git add management-ui docs/management-ui
git commit -m "docs: complete Element Plus migration registry"
```
