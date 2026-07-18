# Management Brand and Density Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the management UI default brand with the supplied BUXOR artwork, display “北京北方新青人窗帘有限公司” in the application shell, and make every management page follow a compact ERP layout without changing business behavior.

**Architecture:** Keep business pages and API contracts intact. Add one source-level brand configuration, apply shared density rules through the existing `function-page-*` and `responsive-page-frame` primitives, then make narrow local fixes only where dashboard, quality, or other special pages cannot use the shared rules safely.

**Tech Stack:** Vue 3, Vite, Element Plus, Tailwind utility classes, Node test runner, Playwright/in-app browser visual verification.

## Global Constraints

- Do not modify backend APIs, permission codes, request payloads, business fields, or workflow behavior.
- Use the supplied `C:\Users\HUAWEI\Desktop\北京北方新青人窗帘布艺有限公司3.jpg` without redrawing, recoloring, or changing the wordmark.
- The application shell company name is exactly `北京北方新青人窗帘有限公司`.
- Desktop first view must include the title, primary statistics, common filters, and the beginning of the table where the page has those sections.
- Validate at 1920×1080, 1440×900, 1366×768, and 390×844.
- Preserve print canvases, label canvases, organization trees, dynamic tables, permission-disabled states, and all loading/error/empty states.
- The final deployment artifact is `C:\Users\HUAWEI\Desktop\hive全新部署` and must contain no `.env`, certificates, runtime data, or test sources.

---

### Task 1: Brand Contract and Static Assets

**Files:**
- Create: `management-ui/src/config/brand.js`
- Create: `management-ui/tests/management-brand-density.test.js`
- Create: `management-ui/images/brand-logo.jpg`
- Create: `management-ui/public/brand-logo.jpg`
- Modify: `management-ui/index.html`
- Modify: `management-ui/src/config/site.js`

**Interfaces:**
- Produces: `brandConfig.companyName`, `brandConfig.logoUrl`, `brandConfig.logoAlt`, and `brandConfig.productName`.
- Consumed by: `Sidebar.vue`, `Navbar.vue`, `Login.vue`, `JoinOrganization.vue`, and `LegalPage.vue`.

- [ ] **Step 1: Write the failing brand contract test**

Add assertions that `brand.js` exports the exact company name and `/brand-logo.jpg`, `index.html` uses `/brand-logo.jpg`, and active source files no longer import `images/logo.png`.

```js
test('management shell uses the approved company brand', () => {
  const brand = read('src/config/brand.js')
  const html = read('index.html')
  assert.match(brand, /companyName:\s*'北京北方新青人窗帘有限公司'/)
  assert.match(brand, /logoUrl:\s*'\/brand-logo\.jpg'/)
  assert.match(html, /href="\/brand-logo\.jpg"/)
})
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `node --test tests/management-brand-density.test.js`

Expected: FAIL because `src/config/brand.js` and the new brand asset do not exist.

- [ ] **Step 3: Add the brand configuration and copy the source artwork**

Create:

```js
export const brandConfig = Object.freeze({
  companyName: '北京北方新青人窗帘有限公司',
  productName: '蜂巢 Hive',
  logoUrl: '/brand-logo.jpg',
  logoAlt: '北京北方新青人窗帘有限公司 Logo'
})
```

Copy the supplied JPG byte-for-byte to both brand asset paths. Update `index.html` favicon and `site.js` company/copyright values to use the approved company name.

- [ ] **Step 4: Run the focused test**

Run: `node --test tests/management-brand-density.test.js`

Expected: PASS for the brand configuration and static asset checks.

- [ ] **Step 5: Commit**

```bash
git add management-ui/index.html management-ui/src/config/brand.js management-ui/src/config/site.js management-ui/images/brand-logo.jpg management-ui/public/brand-logo.jpg management-ui/tests/management-brand-density.test.js
git commit -m "feat: apply approved management brand"
```

### Task 2: Application Shell Branding and Density

**Files:**
- Modify: `management-ui/src/layout/components/Sidebar.vue`
- Modify: `management-ui/src/layout/components/Navbar.vue`
- Modify: `management-ui/src/views/Login.vue`
- Modify: `management-ui/src/views/JoinOrganization.vue`
- Modify: `management-ui/src/views/legal/LegalPage.vue`
- Modify: `management-ui/src/style.css`
- Test: `management-ui/tests/management-brand-density.test.js`
- Test: `management-ui/tests/global-layout-responsiveness.test.js`

**Interfaces:**
- Consumes: `brandConfig` from Task 1.
- Produces: `.app-brand-wordmark`, `.app-company-name`, and compact shared `function-page-*` layout behavior.

- [ ] **Step 1: Extend tests for every shell brand entry and shared density values**

Assert that each shell/auth/legal component imports `brandConfig`, the sidebar no longer imports `images/logo.png`, and the navbar no longer renders “欢迎你” or `currentTenantName` in the company chip. Assert shared page spacing is bounded to compact values.

```js
assert.doesNotMatch(navbar, /tenant-chip__label">欢迎你/)
assert.match(navbar, /brandConfig\.companyName/)
assert.match(style, /--function-section-gap:\s*1rem/)
assert.match(style, /--function-control-height:\s*2\.5rem/)
```

- [ ] **Step 2: Run focused shell and layout tests and verify failure**

Run: `node --test tests/management-brand-density.test.js tests/global-layout-responsiveness.test.js`

Expected: FAIL on old logo imports, old welcome markup, and missing compact tokens.

- [ ] **Step 3: Implement shell branding**

Import `brandConfig` in all listed components. The sidebar uses the supplied wordmark in a wider clipped frame; the top company chip renders only `brandConfig.companyName`; the company chip and logo expose the complete name through `title` and `alt`. Keep tenant code, authentication state, and permission logic unchanged.

- [ ] **Step 4: Implement shared compact layout primitives**

In `style.css`, define compact tokens and apply them to existing page primitives:

```css
.function-page-shell {
  --function-section-gap: 1rem;
  --function-card-gap: 0.875rem;
  --function-control-height: 2.5rem;
}

.function-page-container {
  gap: var(--function-section-gap);
}

.function-filter-form {
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
  gap: 0.75rem;
}
```

Keep touch controls at least 44px tall under the existing mobile breakpoint. Do not target receipt print nodes, label canvases, or organization tree internals.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/management-brand-density.test.js tests/global-layout-responsiveness.test.js tests/global-theme-contrast.test.js`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add management-ui/src/layout/components/Sidebar.vue management-ui/src/layout/components/Navbar.vue management-ui/src/views/Login.vue management-ui/src/views/JoinOrganization.vue management-ui/src/views/legal/LegalPage.vue management-ui/src/style.css management-ui/tests/management-brand-density.test.js
git commit -m "feat: compact the management application shell"
```

### Task 3: Dashboard Layout Repair

**Files:**
- Modify: `management-ui/src/views/dashboard/index.vue`
- Test: `management-ui/tests/management-brand-density.test.js`

**Interfaces:**
- Consumes: shared compact tokens from Task 2.
- Produces: `.dashboard-overview`, `.dashboard-hero`, `.dashboard-quick-grid`, and `.dashboard-summary-grid` layout hooks.

- [ ] **Step 1: Add failing dashboard structure assertions**

Require a two-column hero at wide desktop, a bounded greeting width, a quick-action grid with stable tracks, compact summary cards, and a responsive single-column fallback.

```js
assert.match(dashboard, /class="dashboard-hero"/)
assert.match(dashboard, /class="dashboard-quick-grid"/)
assert.match(dashboard, /@media \(max-width: 1024px\)/)
```

- [ ] **Step 2: Run focused test and verify failure**

Run: `node --test tests/management-brand-density.test.js`

Expected: FAIL because the dashboard still relies on conflicting utility-only sizing.

- [ ] **Step 3: Implement the dashboard structure and local CSS**

Use a `minmax(0, 0.9fr) minmax(34rem, 1.1fr)` hero grid on wide screens, `repeat(3, minmax(0, 1fr))` for the three summary cards, and compact 96–108px summary heights. At `max-width: 1024px`, stack the hero and let quick actions use two or three columns based on available width.

- [ ] **Step 4: Run focused tests**

Run: `node --test tests/management-brand-density.test.js tests/global-layout-responsiveness.test.js`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add management-ui/src/views/dashboard/index.vue management-ui/tests/management-brand-density.test.js
git commit -m "fix: stabilize dashboard information density"
```

### Task 4: Quality Management Layout Repair

**Files:**
- Modify: `management-ui/src/views/function/badProduct/badProduct.vue`
- Test: `management-ui/tests/management-brand-density.test.js`
- Test: `management-ui/tests/element-plus-installation-quality.test.js`

**Interfaces:**
- Consumes: shared filter and stats primitives from Task 2.
- Produces: `.quality-scope-grid`, `.quality-stats-grid`, `.quality-filter-grid`, and `.quality-filter-actions`.

- [ ] **Step 1: Add failing quality density assertions**

Assert that quality uses named compact grids, groups the date fields, keeps action controls in the filter toolbar, and does not use `p-6 text-4xl` statistic cards.

```js
assert.match(quality, /class="quality-stats-grid"/)
assert.match(quality, /class="quality-filter-grid"/)
assert.doesNotMatch(quality, /bg-surface-container-lowest p-6 shadow-sm/)
```

- [ ] **Step 2: Run focused tests and verify failure**

Run: `node --test tests/management-brand-density.test.js tests/element-plus-installation-quality.test.js`

Expected: FAIL on missing compact quality hooks while existing business-contract tests remain green.

- [ ] **Step 3: Implement compact quality sections**

Keep both quality scopes, all query fields, column settings, table columns, dialogs, permissions, and request states. Change only layout: two compact scope buttons, four compact stats, a responsive filter grid with the date controls grouped in one row, and a toolbar immediately followed by the table.

- [ ] **Step 4: Run focused tests**

Run: `node --test tests/management-brand-density.test.js tests/element-plus-installation-quality.test.js`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add management-ui/src/views/function/badProduct/badProduct.vue management-ui/tests/management-brand-density.test.js
git commit -m "fix: compact quality management layout"
```

### Task 5: Full Management Page Density Audit

**Files:**
- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: `management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: `management-ui/src/views/function/inventory/inventory.vue`
- Modify: `management-ui/src/views/function/customer/customer.vue`
- Modify: `management-ui/src/views/function/price/price.vue`
- Modify: `management-ui/src/views/function/employee/employee.vue`
- Modify: `management-ui/src/views/function/organization/organization.vue`
- Modify: `management-ui/src/views/function/equipment/equipment.vue`
- Modify: `management-ui/src/views/function/approval/approvalCenter.vue`
- Modify: `management-ui/src/views/function/attendance/attendanceManagement.vue`
- Modify: `management-ui/src/views/function/announcement/announcement.vue`
- Modify: `management-ui/src/views/function/document/document.vue`
- Modify: `management-ui/src/views/function/role/role.vue`
- Modify: `management-ui/src/views/function/tenant/tenant.vue`
- Modify: `management-ui/src/views/function/label.vue`
- Modify: `management-ui/src/views/function/receipt.vue`
- Test: `management-ui/tests/management-brand-density.test.js`
- Test: existing module tests under `management-ui/tests/element-plus-*.test.js`

**Interfaces:**
- Consumes: shared page primitives from Task 2.
- Produces: every page using either shared compact classes or an explicitly protected domain layout.

- [ ] **Step 1: Add a page audit matrix test**

Create a page list and assert each standard business page contains `function-page-shell` plus either `function-page-header` or an approved protected header class. Assert list pages use a compact filter hook and do not introduce fixed blank spacers such as `min-h-[500px]` around filters.

- [ ] **Step 2: Run the audit test and record failing pages**

Run: `node --test tests/management-brand-density.test.js`

Expected: FAIL listing only pages that still bypass the shared density contract.

- [ ] **Step 3: Apply the page audit matrix**

Standard list pages must use this structure:

```html
<div class="function-page-shell function-page-shell--compact">
  <div class="function-page-container">
    <header class="function-page-header">...</header>
    <section class="function-stats-grid">...</section>
    <section class="function-list-panel">
      <el-form class="function-filter-form">...</el-form>
      <div class="function-table-scroll">...</div>
    </section>
  </div>
</div>
```

Apply `function-page-shell--compact`, `function-stats-grid`, `function-filter-form`, `function-filter-actions`, and `function-table-scroll` to the listed standard surfaces. Receipt, label, order, inventory, and organization may keep their protected domain containers, but their outer header and section gaps must consume the shared compact tokens. Remove only layout spacers; do not change component state, API calls, fields, permission directives, print markup, QR/barcode markup, or dynamic table rendering.

- [ ] **Step 4: Run all management tests**

Run: `npm test`

Expected: all tests PASS with zero failures.

- [ ] **Step 5: Commit**

```bash
git add management-ui/src/views management-ui/tests/management-brand-density.test.js
git commit -m "fix: align management page density"
```

### Task 6: Build and Visual Verification

**Files:**
- Modify: `management-ui/dist/**` through the Vite build only.
- Create: local verification screenshots outside Git.

**Interfaces:**
- Consumes: completed source from Tasks 1–5.
- Produces: verified production `management-ui/dist`.

- [ ] **Step 1: Build the management frontend**

Run: `npm run build`

Expected: Vite exits 0 and writes the production files to `management-ui/dist`.

- [ ] **Step 2: Start a local preview server**

Run: `npm run preview -- --host 127.0.0.1 --port 4173`

Expected: preview is reachable at `http://127.0.0.1:4173`.

- [ ] **Step 3: Capture and inspect key pages**

Use the browser to inspect dashboard, quality, order, inventory, employee, approval, installation, price, equipment, label, and receipt pages at 1920×1080, 1440×900, 1366×768, and 390×844. Verify no overlap, title fragmentation, clipped buttons, hidden company name, or unnecessary filter whitespace.

- [ ] **Step 4: Re-run the complete suite after visual fixes**

Run: `npm test && npm run build`

Expected: zero test failures and successful production build.

- [ ] **Step 5: Commit source and built output**

```bash
git add management-ui/src management-ui/tests management-ui/dist management-ui/index.html management-ui/images management-ui/public
git commit -m "build: publish management brand refresh"
```

### Task 7: Release Metadata and Desktop Deployment Package

**Files:**
- Modify: `deploy/RELEASE_BUILD_INFO.txt`
- Replace: `deploy/management-ui/dist/**`
- Replace: `C:\Users\HUAWEI\Desktop\hive全新部署\management-ui\dist\**`
- Replace: `C:\Users\HUAWEI\Desktop\hive全新部署\RELEASE_BUILD_INFO.txt`

**Interfaces:**
- Consumes: production dist from Task 6 and current Git commit.
- Produces: a clean upload package compatible with `/root/hive` release scripts.

- [ ] **Step 1: Copy the production dist into the repository deployment template**

Replace only `deploy/management-ui/dist` with `management-ui/dist`; do not copy source, tests, `node_modules`, or local screenshots.

- [ ] **Step 2: Recalculate release metadata**

Update `ManagementUiSha256`, `ManagementUiIndexSha256`, `ManagementUiFileCount`, `ManagementUiSourceGitCommit`, and `ReleasePackageGitCommit` using the same sorted SHA-256 algorithm documented in `RELEASE_BUILD_INFO.txt`.

- [ ] **Step 3: Run release and package verification**

Run: `npm test` from `management-ui`, then verify JAR, UI tree, migration catalog, shell LF endings, forbidden paths, and release commit using the repository release scripts or their Windows-equivalent checks.

Expected: all tests pass; backend, UI, and migration hashes match metadata; no `.env`, key, PEM, runtime data, test directory, source directory, or archive is present.

- [ ] **Step 4: Recreate the desktop deployment package**

Rebuild `C:\Users\HUAWEI\Desktop\hive全新部署` from the repository `deploy` allowlist. Preserve only backend artifact, management dist, migrations, Nginx config, scripts, documentation, `.env.example`, Compose file, release metadata, and `publish.sh`.

- [ ] **Step 5: Commit and report the deployment command**

```bash
git add deploy/management-ui/dist deploy/RELEASE_BUILD_INFO.txt
git commit -m "build: package management brand release"
```

Deployment command for files uploaded directly into the server runtime directory:

```bash
cd /root/hive && bash scripts/restart.sh
```
