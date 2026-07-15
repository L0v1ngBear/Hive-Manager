# Global Teal Theme Contrast Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the management web application's old blue interactive theme with the approved teal theme and guarantee readable text and icons in normal, hover, active, loading, and disabled states.

**Architecture:** Treat `management-ui/src/style.css` as the single theme contract for Tailwind utilities, Hive semantic variables, and Element Plus variables. Add a source-level contract test first, then migrate shared shell/authentication styles and business-page hardcodes to semantic variables without changing layout, status colors, print output, permissions, APIs, or workflows.

**Tech Stack:** Vue 3, Element Plus, Tailwind CSS 4, Vite 8, Node.js built-in test runner, CSS custom properties.

## Global Constraints

- Modify only the management web application and its documentation/tests. Do not change the mini-program.
- Preserve page structure, spacing, typography scale, routes, API contracts, permissions, and business behavior.
- Preserve semantic status colors: error red, warning orange, success green, order-stage colors, charts, and print-only black/white rules.
- Use the approved palette: primary `#0F766E`, primary dark `#115E59`, primary container `#CCFBF1`, on-primary-container `#134E4A`, on-primary `#FFFFFF`, main text `#0F172A`, secondary text `#475569`, disabled text `#94A3B8`, disabled background `#E2E8F0`.
- Do not build or copy a deployment package in this change.
- Do not rewrite unrelated user changes. Inspect `git status --short` before every commit.

---

## Task 1: Lock the approved palette and contrast behavior with a failing contract test

**Files:**

- Create: `management-ui/tests/global-theme-contrast.test.js`
- Read: `management-ui/src/style.css`

- [ ] **Step 1: Add the theme token contract test**

Create `management-ui/tests/global-theme-contrast.test.js` with source-level assertions that are stable across CSS formatting:

```js
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const styleSource = readFileSync(new URL('../src/style.css', import.meta.url), 'utf8')
const compact = styleSource.replace(/\s+/g, ' ').toLowerCase()

test('global theme exposes the approved teal semantic tokens', () => {
  assert.match(compact, /--color-primary:\s*#0f766e/)
  assert.match(compact, /--color-primary-container:\s*#ccfbf1/)
  assert.match(compact, /--color-on-primary-container:\s*#134e4a/)
  assert.match(compact, /--ys-primary:\s*#0f766e/)
  assert.match(compact, /--ys-primary-dark:\s*#115e59/)
  assert.match(compact, /--ys-on-primary:\s*#ffffff/)
  assert.match(compact, /--ys-disabled-text:\s*#94a3b8/)
  assert.match(compact, /--ys-disabled-bg:\s*#e2e8f0/)
  assert.match(compact, /--el-color-primary:\s*#0f766e/)
  assert.match(compact, /--primary:\s*15 118 110/)
  assert.match(compact, /--on-primary:\s*255 255 255/)
})

test('deep primary surfaces keep text and icons white', () => {
  assert.match(compact, /\.bg-primary[\s\S]*,[\s\S]*\.el-button--primary[\s\S]*,[\s\S]*\.function-action-primary[\s\S]*\{[^}]*color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(compact, /:is\(\.bg-primary,\s*\.el-button--primary,\s*\.function-action-primary\)[^{]*:is\(\.text-primary,\s*\.text-on-surface,\s*\.text-on-surface-variant,\s*\.material-symbols-outlined\)[^{]*\{[^}]*color:\s*var\(--ys-on-primary\)\s*!important/)
})

test('disabled primary controls use neutral foreground and background colors', () => {
  assert.match(compact, /\.el-button--primary\.is-disabled[^{]*\{[^}]*color:\s*var\(--ys-disabled-text\)\s*!important[^}]*background:\s*var\(--ys-disabled-bg\)\s*!important/)
})
```

Keep the selectors exact so the test proves the intended cascade, not merely the presence of a color elsewhere in the file.

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
cd D:\HiveManager\management-ui
node --test tests/global-theme-contrast.test.js
```

Expected: the new tests fail because `style.css` still declares `#1f3f5f` and lacks the approved semantic/disabled tokens and descendant contrast selector.

- [ ] **Step 3: Commit the failing contract test**

```powershell
cd D:\HiveManager
git add management-ui/tests/global-theme-contrast.test.js
git commit -m "test: define management theme contrast contract"
```

---

## Task 2: Implement the global teal token and contrast layer

**Files:**

- Modify: `management-ui/src/style.css`
- Test: `management-ui/tests/global-theme-contrast.test.js`

- [ ] **Step 1: Replace the root theme tokens**

Update `@theme` and `body` so all frameworks consume the same palette. Use these declarations as the canonical values:

```css
@theme {
  --color-primary: #0f766e;
  --color-primary-container: #ccfbf1;
  --color-on-primary-container: #134e4a;
  --color-secondary: #475569;
  --color-surface-tint: #0f766e;
  --color-on-surface: #0f172a;
  --color-on-surface-variant: #475569;
}

body {
  --ys-primary: #0f766e;
  --ys-primary-dark: #115e59;
  --ys-primary-hover: #0f766e;
  --ys-primary-mid: #3f918b;
  --ys-primary-light: #77b3af;
  --ys-primary-container: #ccfbf1;
  --ys-on-primary-container: #134e4a;
  --ys-on-primary: #ffffff;
  --ys-primary-rgb: 15 118 110;
  --ys-disabled-text: #94a3b8;
  --ys-disabled-bg: #e2e8f0;
  --ys-focus-ring: 0 0 0 3px rgb(var(--ys-primary-rgb) / 0.2);
  --el-color-primary: #0f766e;
  --el-color-primary-light-3: #3f918b;
  --el-color-primary-light-5: #77b3af;
  --el-color-primary-light-7: #a7d5d1;
  --el-color-primary-light-9: #e6f5f3;
  --el-color-primary-dark-2: #115e59;
  --el-text-color-primary: #0f172a;
  --el-text-color-regular: #475569;
  --primary: 15 118 110;
  --on-primary: 255 255 255;
  --on-surface: 15 23 42;
  --on-surface-variant: 71 85 105;
}
```

Retain all unrelated surface, error, accent, sizing, and border tokens.

- [ ] **Step 2: Make primary foreground and disabled precedence explicit**

Place the following rules after the general text utility rules so their `!important` declarations cannot recolor primary-button children:

```css
.bg-primary,
.el-button--primary,
.function-action-primary {
  color: var(--ys-on-primary) !important;
  border-color: transparent !important;
  background: linear-gradient(135deg, var(--ys-primary-dark), var(--ys-primary)) !important;
}

:is(.bg-primary, .el-button--primary, .function-action-primary)
  :is(.text-primary, .text-on-surface, .text-on-surface-variant, .material-symbols-outlined) {
  color: var(--ys-on-primary) !important;
}

.el-button--primary.is-disabled,
.el-button--primary:disabled,
.function-action-primary:disabled {
  color: var(--ys-disabled-text) !important;
  border-color: var(--ys-disabled-bg) !important;
  background: var(--ys-disabled-bg) !important;
  box-shadow: none !important;
}
```

Also convert the global focus outline/pulse, `.text-primary`, `.border-primary`, checkbox/radio/switch/pagination active states, and compatibility ring colors to the new semantic variables. Do not alter red/orange/green business status rules.

- [ ] **Step 3: Run the focused test and confirm GREEN**

```powershell
cd D:\HiveManager\management-ui
node --test tests/global-theme-contrast.test.js
```

Expected: all tests in `global-theme-contrast.test.js` pass.

- [ ] **Step 4: Run the existing Element Plus foundation tests**

```powershell
cd D:\HiveManager\management-ui
node --test tests/element-plus-foundation.test.js tests/element-plus-shell-approval.test.js
```

Expected: zero failures and no selector/contract regression.

- [ ] **Step 5: Commit the global layer**

```powershell
cd D:\HiveManager
git add management-ui/src/style.css management-ui/tests/global-theme-contrast.test.js
git commit -m "feat: apply global teal theme contrast"
```

---

## Task 3: Migrate shared shell, entry pages, and reusable components

**Files:**

- Modify: `management-ui/src/layout/index.vue`
- Modify: `management-ui/src/layout/components/Sidebar.vue`
- Modify: `management-ui/src/layout/components/Navbar.vue`
- Modify: `management-ui/src/views/Login.vue`
- Modify: `management-ui/src/views/JoinOrganization.vue`
- Modify: `management-ui/src/views/ForcePasswordChange.vue`
- Modify: `management-ui/src/views/legal/LegalPage.vue`
- Modify: `management-ui/src/components/BusinessTimeCorrectionPanel.vue`
- Modify: `management-ui/src/components/ComplianceFooter.vue`
- Modify: `management-ui/src/components/GlobalRequestOverlay.vue`
- Modify: `management-ui/src/components/TableColumnSettings.vue`
- Test: `management-ui/tests/global-theme-contrast.test.js`

- [ ] **Step 1: Extend the contract test with a shared-layer old-color scan**

Add a list of the shared files above and assert that none contains the old theme literals:

```js
const oldThemePattern = /#1f3f5f|#0b1f33|rgba\(31,\s*63,\s*95|rgba\(30,\s*64,\s*104/i

test('shared management surfaces do not hardcode the retired blue theme', () => {
  for (const relativePath of sharedThemeFiles) {
    const source = readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
    assert.doesNotMatch(source, oldThemePattern, relativePath)
  }
})
```

Expected before migration: this new test fails and reports the files still using retired blue values.

- [ ] **Step 2: Replace interactive-theme hardcodes with semantic variables**

Use `var(--ys-primary)`, `var(--ys-primary-dark)`, `var(--ys-primary-container)`, `var(--ys-on-primary-container)`, and `rgb(var(--ys-primary-rgb) / <alpha>)` according to meaning. For Tailwind arbitrary values in Vue templates, use direct approved teal values where CSS variables are not supported by the generated class, for example:

```html
<div class="bg-[radial-gradient(circle_at_50%_12%,rgba(15,118,110,0.14),transparent_34%),linear-gradient(180deg,#ffffff_0%,#f5f7fb_100%)]"></div>
```

Update login QR/logo fills from `#1f3f5f` to `#0f766e`. Keep legal text, surfaces, structure, and authentication behavior unchanged.

- [ ] **Step 3: Run shared-layer tests**

```powershell
cd D:\HiveManager\management-ui
node --test tests/global-theme-contrast.test.js tests/element-plus-foundation.test.js tests/element-plus-shell-approval.test.js tests/auth-storage-security.test.js
```

Expected: zero failures.

- [ ] **Step 4: Commit the shared migration**

```powershell
cd D:\HiveManager
git add management-ui/src/layout management-ui/src/views/Login.vue management-ui/src/views/JoinOrganization.vue management-ui/src/views/ForcePasswordChange.vue management-ui/src/views/legal/LegalPage.vue management-ui/src/components/BusinessTimeCorrectionPanel.vue management-ui/src/components/ComplianceFooter.vue management-ui/src/components/GlobalRequestOverlay.vue management-ui/src/components/TableColumnSettings.vue management-ui/tests/global-theme-contrast.test.js
git commit -m "refactor: migrate shared web surfaces to teal theme"
```

---

## Task 4: Migrate business-page theme hardcodes without changing status or print colors

**Files:**

- Modify: `management-ui/src/views/manual/UserManual.vue`
- Modify: `management-ui/src/views/function/badProduct/badProduct.vue`
- Modify: `management-ui/src/views/function/installationTask/installationTask.vue`
- Modify: `management-ui/src/views/function/order/order.vue`
- Modify: `management-ui/src/views/function/role/permissionDrawer.vue`
- Modify: `management-ui/src/views/function/employee/employee.vue`
- Modify: `management-ui/src/views/function/employee/EmployeePermissionDrawer.vue`
- Modify: `management-ui/src/views/function/receipt.vue`
- Modify: `management-ui/src/views/function/label.vue`
- Test: `management-ui/tests/global-theme-contrast.test.js`

- [ ] **Step 1: Add the business-page scan to the theme contract test**

Add the listed business files to `businessThemeFiles` and scan them with `oldThemePattern`. For `receipt.vue` and `label.vue`, scan only the interactive stylesheet source after excluding documented print sections between `@media print {` and their matching closing blocks. Use this brace-aware helper so nested print rules cannot leak into the scan:

```js
function stripPrintMediaBlocks(source) {
  let result = ''
  let cursor = 0

  while (cursor < source.length) {
    const mediaStart = source.indexOf('@media print', cursor)
    if (mediaStart === -1) return result + source.slice(cursor)

    result += source.slice(cursor, mediaStart)
    const blockStart = source.indexOf('{', mediaStart)
    assert.notEqual(blockStart, -1, 'print media block must have an opening brace')

    let depth = 1
    let blockEnd = blockStart + 1
    while (blockEnd < source.length && depth > 0) {
      if (source[blockEnd] === '{') depth += 1
      if (source[blockEnd] === '}') depth -= 1
      blockEnd += 1
    }
    assert.equal(depth, 0, 'print media block must have balanced braces')
    cursor = blockEnd
  }

  return result
}
```

The test must continue allowing black/white print colors, order status variables, warning orange, error red, and success green.

- [ ] **Step 2: Migrate page-local theme declarations by meaning**

Apply these mappings only to interactive theme uses:

```text
#1f3f5f                    -> var(--ys-primary)
#0b1f33                    -> var(--ys-primary-dark)
rgba(31, 63, 95, alpha)   -> rgb(var(--ys-primary-rgb) / alpha)
old blue primary gradient -> linear-gradient(135deg, var(--ys-primary-dark), var(--ys-primary))
```

Where a local CSS custom property represents an accent, keep the property and change only its value, for example:

```css
.summary-card {
  --summary-color: var(--ys-primary);
  --summary-bg: rgb(var(--ys-primary-rgb) / 0.08);
}
```

Do not replace colors inside print/canvas payload generation, QR/barcode rendering, chart series, invoice/order status chips, or warning/error/success classes unless the value is explicitly the retired global primary.

- [ ] **Step 3: Re-scan the complete management source**

```powershell
cd D:\HiveManager
rg -n -e "#1f3f5f|#0b1f33|rgba\(31,\s*63,\s*95|rgba\(30,\s*64,\s*104" management-ui/src
```

Expected: no matches in interactive management UI source. Any retained print-only match must be documented with file and reason in `docs/management-ui/modules/shared-foundation.md` before proceeding.

- [ ] **Step 4: Run page-focused tests**

```powershell
cd D:\HiveManager\management-ui
node --test tests/global-theme-contrast.test.js tests/element-plus-order.test.js tests/element-plus-installation-quality.test.js tests/element-plus-printing.test.js tests/element-plus-employee-attendance.test.js tests/element-plus-manual.test.js
```

Expected: zero failures.

- [ ] **Step 5: Commit the business-page migration**

```powershell
cd D:\HiveManager
git add management-ui/src/views management-ui/tests/global-theme-contrast.test.js
git commit -m "refactor: migrate business pages to teal theme"
```

---

## Task 5: Document, build, and visually verify the complete theme

**Files:**

- Modify: `docs/management-ui/modules/shared-foundation.md`
- Modify: `docs/management-ui/modules/layout-navigation.md`
- Modify: `docs/management-ui/modules/authentication.md`
- Modify: `docs/management-ui/README.md`
- Verify: `management-ui/src/style.css`
- Verify: `management-ui/src/**/*.vue`
- Verify: `management-ui/tests/global-theme-contrast.test.js`

- [ ] **Step 1: Document the canonical palette and cascade rule**

Record the approved semantic tokens, the rule that deep-primary children remain white, the neutral disabled-state precedence, and the exclusion of status/print colors in `shared-foundation.md`. Record shell/auth migration in their module documents. Update the README verification section with the actual final test/build result rather than preserving an obsolete test count.

- [ ] **Step 2: Run the complete management UI test suite**

```powershell
cd D:\HiveManager\management-ui
npm test
```

Expected: every Node test passes with zero failures.

- [ ] **Step 3: Run the production build**

```powershell
cd D:\HiveManager\management-ui
npm run build
```

Expected: Vite exits with code 0 and writes `management-ui/dist` without CSS or Vue compilation errors.

- [ ] **Step 4: Start the local management UI for browser QA**

```powershell
cd D:\HiveManager\management-ui
npm run dev -- --host 127.0.0.1 --port 5173
```

Use the browser to inspect at least the login screen and, when the local API/session allows access, dashboard, order, employee permissions, approval, inventory, installation task, label, and receipt pages at `1440x900`, `1024x768`, and `390x844`.

Acceptance checks:

- Primary buttons, selected navigation, loading icons, and primary badges use teal with readable foregrounds.
- White text/icons remain white inside deep primary surfaces, including descendants carrying global text utilities.
- Disabled controls use neutral gray and remain legible.
- Status colors keep their existing business meaning.
- No text overlap or layout movement is introduced.
- Print previews retain their existing black/white output.

If authenticated pages cannot be reached because the local backend is unavailable, record that limitation in `docs/management-ui/README.md`; do not claim those pages were visually approved.

- [ ] **Step 5: Run final repository checks**

```powershell
cd D:\HiveManager
git diff --check
$markers = @('TO' + 'DO', 'T' + 'BD', 'PLACE' + 'HOLDER', 'coming' + ' soon')
Select-String -Path docs/superpowers/plans/2026-07-15-global-teal-theme-contrast.md,management-ui/tests/global-theme-contrast.test.js -Pattern $markers
git status --short
```

Expected: `git diff --check` succeeds, the placeholder scan returns no matches, and status contains only intentional theme/test/documentation changes plus any pre-existing unrelated user changes.

- [ ] **Step 6: Commit documentation and final verification updates**

```powershell
cd D:\HiveManager
git add docs/management-ui/README.md docs/management-ui/modules/shared-foundation.md docs/management-ui/modules/layout-navigation.md docs/management-ui/modules/authentication.md
git commit -m "docs: record management teal theme contract"
```

Do not create or refresh the deployment directory after this commit.
