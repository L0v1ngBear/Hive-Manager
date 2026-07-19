# Customer and Login Layout Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair the customer-management layout and replace the login composition with the approved standard enterprise split layout without changing business behavior or public copy.

**Architecture:** Keep the customer title/action header independent from the collapsible filter container, then place filters, table, and pagination in one vertical list panel. Recompose `Login.vue` into brand and authentication panels, using a local `loginMode` view state to switch between the existing account and QR flows while retaining the existing authentication functions.

**Tech Stack:** Vue 3 `<script setup>`, Element Plus, Tailwind utility classes, component-scoped CSS, Node.js built-in test runner, Vite 8.

## Global Constraints

- Existing customer API, permissions, filtering, paging, detail, edit, and export behavior must not change.
- Existing login API, QR session polling, password reset, login redirect, remember-account behavior, and error copy must not change.
- Existing login-page brand, product text, legal links, and registration information must not be replaced.
- Desktop target is a two-column login shell; narrow screens must use one column without horizontal overflow.
- Release output remains the uncompressed directory `C:\Users\HUAWEI\Desktop\hive全新部署`; do not deploy remotely.
- The isolated Windows worktree has one known baseline-only failure because `release-runtime-safety.test.js` assumes LF in `deploy/publish.sh`; page-specific tests, build, and the post-merge main-branch full suite must pass.

## File Map

- `management-ui/src/views/function/customer/customer.vue`: owns the customer title, summary, filters, table, pagination, and customer dialogs.
- `management-ui/tests/element-plus-customer-document.test.js`: holds customer list structure and behavior contracts.
- `management-ui/src/views/Login.vue`: owns account login, QR login, reset-password dialog, and login-page presentation.
- `management-ui/tests/login-layout.test.js`: new structural and responsive contract for the approved login composition.
- `management-ui/dist/**`: generated production frontend copied to the fixed desktop release directory only after tests pass.
- `management-ui/dist-manifest.sha256` and desktop `RELEASE_BUILD_INFO.txt`: generated release integrity metadata.

---

### Task 1: Repair the customer-management page structure

**Files:**
- Modify: `management-ui/tests/element-plus-customer-document.test.js`
- Modify: `management-ui/src/views/function/customer/customer.vue:1-177`
- Modify: `management-ui/src/views/function/customer/customer.vue:518` (append scoped styles)

**Interfaces:**
- Consumes: existing `filters`, `handleFilter`, `resetFilter`, `visibleCustomerColumns`, `openCreateDrawer`, and permission computed values.
- Produces: stable DOM contracts `.customer-page-header`, `.customer-summary-grid`, `.customer-list-panel`, and `.customer-filter-form`; the existing table retains the same row and column renderers.

- [ ] **Step 1: Write the failing customer layout test**

Append this test to `management-ui/tests/element-plus-customer-document.test.js`:

```js
test('customer list separates header actions from collapsible filters', () => {
  const customer = read('../src/views/function/customer/customer.vue')
  const header = customer.match(/<header class="customer-page-header[\s\S]*?<\/header>/)?.[0] || ''
  const listPanel = customer.match(/<section class="customer-list-panel[\s\S]*?<CustomerCreateDrawer/)?.[0] || ''

  assert.match(header, /@click="openCreateDrawer"/)
  assert.doesNotMatch(header, /v-filter-collapse|class="function-filter-form/)
  assert.match(listPanel, /v-filter-collapse class="function-filter-form customer-filter-form"/)
  assert.match(listPanel, /class="function-table-scroll responsive-table-wrap"/)
  assert.match(listPanel, /<el-table-column label="操作" fixed="right" width="128"/)
  assert.match(customer, /class="customer-summary-grid"/)
  assert.match(customer, /\.customer-filter-form\s*\{[\s\S]*grid-template-columns/)
  assert.match(customer, /@media \(max-width: 900px\)[\s\S]*\.customer-filter-form/)
  assert.match(customer, /@media \(max-width: 640px\)[\s\S]*\.customer-filter-form/)
})
```

- [ ] **Step 2: Run the customer test and verify RED**

Run:

```powershell
node --test tests/element-plus-customer-document.test.js
```

Expected: FAIL because `customer-page-header`, `customer-list-panel`, the relocated filter form, and fixed operation column are absent.

- [ ] **Step 3: Move the create action into the header**

Change the header to this contract, retaining the existing title block and button bindings:

```vue
<header class="customer-page-header function-page-header">
  <div>
    <div class="function-page-eyebrow">
      <span class="material-symbols-outlined">handshake</span>
      客户经营中心
    </div>
    <h1 class="function-page-title">客户档案库</h1>
    <p class="function-page-desc">管理客户基础信息、联系人和合作项目，施工区域按项目维度维护。</p>
  </div>
  <el-button
    class="customer-create-action"
    type="primary"
    :disabled="!canCreateCustomer"
    :class="permissionDisabledClass(!canCreateCustomer)"
    :title="canCreateCustomer ? '新建客户' : '当前账号暂无新增客户权限'"
    @click="openCreateDrawer"
  >
    <span class="material-symbols-outlined text-[20px]">domain_add</span>
    新建客户
  </el-button>
</header>
```

- [ ] **Step 4: Place summary, filters, table, and pagination in stable containers**

Apply these exact edits:

1. Cut the current filter container from `<div v-filter-collapse class="function-filter-form">` through its closing `</div>` after `TableColumnSettings`, excluding the new-customer button that Task 1 Step 3 moved.
2. Paste that container immediately inside the list panel, before `el-result`, and change its class to `function-filter-form customer-filter-form`.
3. Change the summary section class from `function-stats-grid grid-cols-1 md:grid-cols-4` to `customer-summary-grid`.
4. Change the list section class from `function-list-panel relative shadow-sm border-outline-variant/20 bg-surface-container-lowest` to `customer-list-panel function-list-panel relative shadow-sm border-outline-variant/20 bg-surface-container-lowest`.
5. Change the table wrapper class from `function-table-scroll` to `function-table-scroll responsive-table-wrap`.
6. Add `fixed="right"` to the existing operation column.

The resulting summary and list openings must be:

```vue
<section class="customer-summary-grid">
  <div class="function-stat-card group relative overflow-hidden bg-primary-container">
    <div class="absolute top-0 right-0 p-4 opacity-10 transition-transform group-hover:scale-110">
      <span class="material-symbols-outlined text-[80px]">corporate_fare</span>
    </div>
    <p class="text-xs font-bold tracking-widest text-on-primary/80 uppercase">客户总数</p>
    <h3 class="mt-2 text-2xl font-black text-white">{{ total }}</h3>
  </div>
</section>

<section class="customer-list-panel function-list-panel relative shadow-sm border-outline-variant/20 bg-surface-container-lowest">
  <div v-filter-collapse class="function-filter-form customer-filter-form">
    <el-input v-model.trim="filters.keyword" placeholder="搜索客户名称、联系人、项目或负责人" clearable @keyup.enter="handleFilter" />
    <el-select v-model="filters.customerType" placeholder="全部客户类型" clearable :value-on-clear="''" />
    <el-date-picker v-model="filters.createStart" type="date" value-format="YYYY-MM-DD" placeholder="创建开始" />
    <el-date-picker v-model="filters.createEnd" type="date" value-format="YYYY-MM-DD" placeholder="创建结束" />
    <div class="function-filter-actions"></div>
  </div>
</section>
```

The compact snippet above specifies node order; retain the current input prefix icon, customer-type options, button children, `TableColumnSettings` props, list error, table cell templates, empty state, and pagination byte-for-byte when moving them. Set the table wrapper and operation column openings to:

```vue
<div class="function-table-scroll responsive-table-wrap">
  <el-table :data="customerList" row-key="id" v-loading="loading" class="w-full" @row-click="handleCustomerRowClick">
    <el-table-column label="操作" fixed="right" width="128" align="right">
    </el-table-column>
  </el-table>
</div>
```

- [ ] **Step 5: Add customer-specific responsive CSS**

Append:

```vue
<style scoped>
.customer-summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 24rem);
}

.customer-filter-form {
  grid-template-columns: minmax(16rem, 1.5fr) repeat(3, minmax(10rem, 1fr)) minmax(15rem, auto);
  padding: 1rem;
  border-bottom: 1px solid rgba(200, 211, 223, 0.64);
}

.customer-create-action {
  flex: 0 0 auto;
}

@media (max-width: 900px) {
  .customer-filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .customer-summary-grid,
  .customer-filter-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .customer-create-action,
  .customer-filter-form .function-filter-actions {
    width: 100%;
  }
}
</style>
```

- [ ] **Step 6: Run customer tests and verify GREEN**

Run:

```powershell
node --test tests/element-plus-customer-document.test.js tests/global-layout-responsiveness.test.js
```

Expected: all selected tests PASS.

- [ ] **Step 7: Commit the customer fix**

```powershell
git add management-ui/src/views/function/customer/customer.vue management-ui/tests/element-plus-customer-document.test.js
git commit -m "fix: stabilize customer management layout"
```

---

### Task 2: Implement the approved enterprise split login layout

**Files:**
- Create: `management-ui/tests/login-layout.test.js`
- Modify: `management-ui/src/views/Login.vue:1-166`
- Modify: `management-ui/src/views/Login.vue:652-687`

**Interfaces:**
- Consumes: existing `handleLogin`, `goJoinOrganization`, `scanSession`, `scanStatus`, `scanStatusText`, password-reset functions, and `brandConfig`.
- Produces: local `loginMode: Ref<'account' | 'scan'>`; stable DOM contracts `.login-shell`, `.login-brand-panel`, `.login-auth-panel`, `.login-mode-tabs`, and `.login-scan-panel`.

- [ ] **Step 1: Write the failing login layout contract**

Create `management-ui/tests/login-layout.test.js`:

```js
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/Login.vue', import.meta.url), 'utf8')

test('login uses the approved split layout and preserves both login modes', () => {
  assert.match(source, /class="login-shell"/)
  assert.match(source, /class="login-brand-panel"/)
  assert.match(source, /class="login-auth-panel"/)
  assert.match(source, /const loginMode = ref\('account'\)/)
  assert.match(source, /role="tablist"/)
  assert.match(source, /@click="loginMode = 'account'"/)
  assert.match(source, /@click="loginMode = 'scan'"/)
  assert.match(source, /v-if="loginMode === 'account'"/)
  assert.match(source, /class="login-scan-panel"/)
  assert.match(source, /@submit\.prevent="handleLogin"/)
  assert.match(source, /@click="goJoinOrganization"/)
  assert.match(source, /首次登录 \/ 忘记密码/)
  assert.match(source, /scanSession\.qrCodeDataUrl/)
})

test('login split layout collapses to one column on narrow screens', () => {
  assert.match(source, /\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1\.04fr\)\s+minmax\(0,\s*0\.96fr\)/)
  assert.match(source, /@media \(max-width: 900px\)[\s\S]*\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/)
  assert.match(source, /@media \(max-width: 640px\)[\s\S]*\.login-stage/)
})
```

- [ ] **Step 2: Run the login test and verify RED**

Run:

```powershell
node --test tests/login-layout.test.js
```

Expected: FAIL because the split shell, login mode state, tablist, and responsive contracts do not exist.

- [ ] **Step 3: Add login mode state**

Add beside the existing state refs:

```js
const loginMode = ref('account')
```

Retain all existing authentication and QR session functions. Simplify the purely decorative character state only after the new template no longer references it; keep `isError` and `errorMessage` for login feedback.

- [ ] **Step 4: Replace the login presentation with the approved shell**

Apply these exact moves before writing the new wrapper:

1. Retain the existing reset-password dialog starting at `<el-dialog v-model="resetDialogVisible"` without changing its text, inputs, or handlers.
2. Move the existing account form starting at `<el-form :model="loginForm"` into the new authentication panel and add `v-if="loginMode === 'account'"`.
3. Move both existing QR branches (`scanStatus === 'CONFIRMED'` and its `v-else`) into a new `<div v-else class="login-scan-panel">`.
4. Keep every existing binding inside the moved form and QR branches, including `handleLogin`, `openResetPasswordDialog`, `goJoinOrganization`, `scanSession.qrCodeDataUrl`, `scanStatus`, and `scanStatusText`.
5. Remove only the decorative pixel-character SVG nodes after their refs and pointer-tracking state are no longer referenced.

Use this new outer structure and tablist:

```vue
<main class="login-stage">
  <section class="login-shell">
    <aside class="login-brand-panel">
      <div class="login-brand-lockup">
        <img :src="brandConfig.logoUrl" :alt="brandConfig.logoAlt" class="brand-logo-image" />
        <span>{{ brandConfig.productName }}</span>
      </div>
      <div class="login-brand-copy">
        <p class="login-kicker">企业信息管理</p>
        <h1>让生产协同更清晰、更高效</h1>
        <p>专业、高效、可靠、价值，协同工业生产效率。把经验和流程变成可追踪、可复盘、可优化的数据资产。</p>
      </div>
      <ul class="login-benefits">
        <li>统一业务协同</li>
        <li>安全权限控制</li>
        <li>生产过程可追踪</li>
      </ul>
    </aside>

    <section class="login-auth-panel">
      <div>
        <h2>欢迎登录</h2>
        <p>请输入账号信息进入系统</p>
      </div>
      <div class="login-mode-tabs" role="tablist" aria-label="登录方式">
        <button type="button" role="tab" :aria-selected="loginMode === 'account'" @click="loginMode = 'account'">账号登录</button>
        <button type="button" role="tab" :aria-selected="loginMode === 'scan'" @click="loginMode = 'scan'">扫码登录</button>
      </div>
      <el-form v-if="loginMode === 'account'" :model="loginForm" label-position="top" @submit.prevent="handleLogin">
      </el-form>
      <div v-else class="login-scan-panel">
      </div>
    </section>
  </section>
</main>
```

The empty form and scan nodes in this structural snippet mark the exact insertion points for the existing complete nodes identified in items 2 and 3; they are not new empty production containers.

- [ ] **Step 5: Add the approved desktop and mobile styles**

Replace the obsolete pixel-character styles with component-scoped rules that include these contracts:

```css
.login-stage {
  min-height: 100%;
  display: grid;
  place-items: center;
  overflow-x: hidden;
  padding: clamp(1rem, 4vw, 3rem);
  background: radial-gradient(circle at 12% 12%, rgba(15, 118, 110, 0.16), transparent 34%), #f5f7fb;
}

.login-shell {
  width: min(100%, 68rem);
  min-height: 42rem;
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(0, 0.96fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 1.75rem;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 32px 72px rgba(15, 23, 42, 0.14);
}

.login-brand-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(2rem, 5vw, 4rem);
  color: white;
  background: linear-gradient(145deg, #0f766e, #134e4a);
}

.login-auth-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: clamp(2rem, 5vw, 4rem);
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .login-brand-panel {
    min-height: 18rem;
  }
}

@media (max-width: 640px) {
  .login-stage {
    place-items: start center;
    padding: 0;
  }

  .login-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }
}
```

- [ ] **Step 6: Run login and authentication tests and verify GREEN**

Run:

```powershell
node --test tests/login-layout.test.js tests/unified-employee-login-ui.test.js tests/auth-storage-security.test.js tests/element-plus-shell-approval.test.js tests/global-theme-contrast.test.js
```

Expected: all selected tests PASS.

- [ ] **Step 7: Commit the login layout**

```powershell
git add management-ui/src/views/Login.vue management-ui/tests/login-layout.test.js
git commit -m "feat: align login page with enterprise layout"
```

---

### Task 3: Verify responsive behavior and produce the frontend release

**Files:**
- Generated: `management-ui/dist/**`
- Generated: `management-ui/dist-manifest.sha256`
- Update delivery: `C:\Users\HUAWEI\Desktop\hive全新部署\management-ui\dist/**`
- Update delivery: `C:\Users\HUAWEI\Desktop\hive全新部署\management-ui\dist-manifest.sha256`
- Update delivery: `C:\Users\HUAWEI\Desktop\hive全新部署\RELEASE_BUILD_INFO.txt`

**Interfaces:**
- Consumes: the customer and login components from Tasks 1 and 2.
- Produces: a verified Vite build and desktop release tree whose UI manifest matches the generated files.

- [ ] **Step 1: Run all page-specific regression tests**

```powershell
node --test tests/element-plus-customer-document.test.js tests/global-layout-responsiveness.test.js tests/login-layout.test.js tests/unified-employee-login-ui.test.js tests/auth-storage-security.test.js tests/element-plus-shell-approval.test.js tests/global-theme-contrast.test.js
```

Expected: all selected tests PASS.

- [ ] **Step 2: Run the worktree frontend suite**

```powershell
npm test -- --test-reporter=dot
```

Expected: every frontend test passes except the documented CRLF-only `root publish wrapper delegates to the guarded synchronization and restart entrypoints` baseline failure. No page-related test may fail.

- [ ] **Step 3: Build production assets**

```powershell
npm run build
```

Expected: Vite exits 0, `dist/index.html` exists, and `scripts/write-dist-manifest.mjs` reports the generated file count.

- [ ] **Step 4: Perform viewport checks**

Run the built frontend locally and inspect `/login` and `/function/customer` at 1440px, 1024px, and 390px. For the authenticated customer route, use a local test session or captured component harness; do not submit or modify production data.

Expected at every width: no horizontal page overflow, no overlapping filters, visible customer action column, working filter collapse, and a one-column login shell at 900px and below.

- [ ] **Step 5: Review the branch diff**

```powershell
git diff --check main...HEAD
git diff --stat main...HEAD
git status --short
```

Expected: no whitespace errors; only the approved design, plan, customer component/test, and login component/test are tracked changes.

- [ ] **Step 6: Merge the feature branch into main**

Use the `superpowers:finishing-a-development-branch` workflow. Merge only after review and selected tests/build pass.

- [ ] **Step 7: Run the complete suite on main**

From `D:\HiveManager\management-ui` after merge:

```powershell
npm run build
npm test -- --test-reporter=dot
```

Expected: build exits 0 and the full suite passes with zero failures because the main checkout preserves the publish script's LF line endings.

- [ ] **Step 8: Refresh desktop UI files and metadata**

Mirror the verified `D:\HiveManager\management-ui\dist` into `C:\Users\HUAWEI\Desktop\hive全新部署\management-ui\dist`, copy the generated `dist-manifest.sha256`, then update only the management UI build time, source commit, SHA-256, index SHA-256, manifest SHA-256, and file count fields in desktop `RELEASE_BUILD_INFO.txt`. Keep the backend JAR and migration metadata unchanged.

- [ ] **Step 9: Verify desktop integrity**

Recompute the UI manifest from the desktop `management-ui/dist`, compare it byte-for-byte with `management-ui/dist-manifest.sha256`, and verify the result equals `ManagementUiSha256` and `ManagementUiManifestSha256`. Confirm the desktop tree contains no `.env`, certificates, source directories, `node_modules`, or runtime data.

Expected: all hashes and counts match, the existing backend JAR hash remains unchanged, and the release directory is ready for the user's overwrite workflow.
