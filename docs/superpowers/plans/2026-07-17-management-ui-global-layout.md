# Management UI Global Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Hive 管理端全部页面的全局响应式、筛选、表格、侧栏、权限选择器和基础无障碍问题。

**Architecture:** 以 `ResponsivePageFrame` 和 `src/style.css` 为唯一全局响应式入口，移除覆盖所有业务网格的宽泛规则，新增具名布局契约供页面复用。订单、价格、设备和权限页面补充自身需要的语义结构，不改业务接口和状态流转。

**Tech Stack:** Vue 3、Element Plus、Tailwind CSS 4、Vite 8、Node.js `node:test`

## Global Constraints

- 不改变业务接口、权限码、数据库结构、订单状态流转或后端逻辑。
- 桌面、866px 窄桌面和 390px 手机视口都必须验证。
- 不新增 UI 依赖；继续使用 Element Plus 和现有 Material Symbols。
- 所有生产代码变更必须先有能复现问题的失败测试。

---

## File Map

- `management-ui/src/style.css`：共用断点、统计网格、筛选布局和表格溢出契约。
- `management-ui/src/layout/components/Sidebar.vue`：桌面默认展开、折叠图标提示和品牌文案。
- `management-ui/src/views/function/price/price.vue`：价格统计卡、筛选网格、表格滚动和筛选可访问名称。
- `management-ui/src/views/function/equipment/equipment.vue`：设备筛选网格、稳定控件宽度、表格滚动和筛选可访问名称。
- `management-ui/src/views/function/order/order.vue`：订单统计分组、筛选展开、日期布局、列表密度和手机首屏。
- `management-ui/src/views/function/role/permissionDrawer.vue`：可搜索、分组、只看已选的权限列表和可访问标题。
- `management-ui/src/views/function/role/permissionPresentation.js`：权限树展示转换的纯函数。
- `management-ui/tests/global-layout-responsiveness.test.js`：全局样式与页面契约回归测试。
- `management-ui/tests/permission-presentation.test.js`：权限分组、搜索和已选过滤单元测试。
- `docs/audits/2026-07-17-page-layout/AUDIT.md`：修复结果和新截图索引。

### Task 1: Add Failing Global Layout Contracts

**Files:**
- Create: `management-ui/tests/global-layout-responsiveness.test.js`
- Read: `management-ui/src/style.css`
- Read: `management-ui/src/layout/components/Sidebar.vue`
- Read: `management-ui/src/views/function/price/price.vue`
- Read: `management-ui/src/views/function/equipment/equipment.vue`

**Interfaces:**
- Consumes: Vue source and CSS as UTF-8 text.
- Produces: source-level contracts that fail on the current broad 900px rule, collapsed sidebar labels, missing page classes and wrong brand.

- [ ] **Step 1: Write the failing contract test**

```js
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const style = read('src/style.css')
const sidebar = read('src/layout/components/Sidebar.vue')
const price = read('src/views/function/price/price.vue')
const equipment = read('src/views/function/equipment/equipment.vue')
const order = read('src/views/function/order/order.vue')

test('tablet rules do not force every direct business grid to one column', () => {
  assert.doesNotMatch(style, /\.function-page-container\s*>\s*\.grid[\s\S]{0,260}grid-template-columns:\s*minmax\(0,\s*1fr\)\s*!important/)
})

test('shared list layouts expose stable stats filters and horizontal tables', () => {
  assert.match(style, /\.function-stats-grid/)
  assert.match(style, /\.function-filter-form/)
  assert.match(style, /\.function-table-scroll/)
  assert.match(price, /class="function-stats-grid"/)
  assert.match(price, /class="function-filter-form"/)
  assert.match(equipment, /class="function-filter-form/)
})

test('desktop sidebar opens by default and collapsed navigation is icon only', () => {
  assert.match(sidebar, /const isCollapsed = ref\(false\)/)
  assert.doesNotMatch(sidebar, /isCollapsed \? 'text-\[10px\]/)
  assert.match(sidebar, /v-if="!isCollapsed"[\s\S]{0,140}item\.name/)
  assert.match(sidebar, /:content="item\.name"/)
})

test('Hive branding is consistent', () => {
  assert.doesNotMatch(sidebar, /轻巢 Hive/)
  assert.match(sidebar, /蜂巢 Hive/)
})

test('order page defines responsive summary filters and a compact mobile entry', () => {
  assert.match(order, /order-summary-grid-new/)
  assert.match(order, /order-filter-grid/)
  assert.match(order, /order-mobile-summary-toggle/)
})
```

- [ ] **Step 2: Run the test and verify RED**

Run: `npm test -- tests/global-layout-responsiveness.test.js` from `management-ui`.

Expected: FAIL for the broad direct-grid selector, missing shared classes, collapsed sidebar behavior and “轻巢 Hive”.

- [ ] **Step 3: Commit the red test**

```bash
git add management-ui/tests/global-layout-responsiveness.test.js
git commit -m "test: define management layout responsiveness contracts"
```

### Task 2: Repair the Shared Responsive Foundation

**Files:**
- Modify: `management-ui/src/style.css:518-706`
- Test: `management-ui/tests/global-layout-responsiveness.test.js`

**Interfaces:**
- Consumes: `ResponsivePageFrame` route name through `data-route-name`.
- Produces: `.function-stats-grid`, `.function-filter-form`, `.function-filter-actions`, `.function-table-scroll`.

- [ ] **Step 1: Replace the broad grid and table fallback**

Use these contracts in `src/style.css`:

```css
.function-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--responsive-card-gap);
}

.function-filter-form {
  display: grid !important;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  align-items: end;
  gap: 0.75rem 1rem;
}

.function-filter-form .el-form-item {
  width: 100%;
  min-width: 0;
  margin: 0 !important;
}

.function-filter-form :is(.el-input, .el-select, .el-date-editor, .el-input-number) {
  width: 100% !important;
  min-width: 0;
}

.function-filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.function-table-scroll,
.function-page-shell .responsive-table-wrap {
  width: 100%;
  min-width: 0;
  overflow-x: auto !important;
  scrollbar-gutter: stable;
}

@media (max-width: 900px) {
  .function-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 640px) {
  .function-stats-grid,
  .function-filter-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
```

Move the native `.responsive-data-table` card conversion from `max-width: 900px` to `max-width: 640px`. Keep table headers and horizontal scrolling between 641px and 900px. Correct the mobile action fallback text from the corrupted value to `操作`.

- [ ] **Step 2: Run the focused test and verify GREEN for shared CSS assertions**

Run: `npm test -- tests/global-layout-responsiveness.test.js`.

Expected: sidebar/page assertions still fail; broad-grid and shared CSS assertions pass.

- [ ] **Step 3: Run existing theme tests**

Run: `node --test tests/global-theme-contrast.test.js`.

Expected: PASS with no contrast or CSS scanner regression.

- [ ] **Step 4: Commit the responsive foundation**

```bash
git add management-ui/src/style.css management-ui/tests/global-layout-responsiveness.test.js
git commit -m "fix: scope management responsive layout rules"
```

### Task 3: Repair Sidebar Layout and Branding

**Files:**
- Modify: `management-ui/src/layout/components/Sidebar.vue:31-121,147-153`
- Test: `management-ui/tests/global-layout-responsiveness.test.js`

**Interfaces:**
- Consumes: `mobile` prop and existing menu items.
- Produces: expanded desktop default, icon-only collapsed state, Element Plus tooltips and “蜂巢 Hive” copy.

- [ ] **Step 1: Implement icon-only collapsed menu items**

Set `const isCollapsed = ref(false)`. Wrap primary and secondary menu buttons in `el-tooltip` with `:disabled="!isCollapsed"`, `:content="item.name"`, `placement="right"`. Render menu text only with `v-if="!isCollapsed"`. For the “更多功能” button, render only the `apps` icon when collapsed. Replace the collapsed “展开” text with an icon-only button whose `aria-label` and tooltip are `展开导航`.

Import `ElTooltip` with the existing Element Plus imports.

- [ ] **Step 2: Correct brand copy**

```js
const brandLogoAlt = computed(() => tenantLogoUrl.value ? `${tenantName.value} logo` : '蜂巢 Hive logo')
const brandTitle = computed(() => tenantLogoUrl.value && !userStore.isPlatformTenant ? tenantName.value : '蜂巢 Hive')
```

- [ ] **Step 3: Run focused and shell tests**

Run: `node --test tests/global-layout-responsiveness.test.js tests/element-plus-shell-approval.test.js`.

Expected: sidebar and brand assertions pass; page-class assertions remain until Task 4.

- [ ] **Step 4: Commit the sidebar repair**

```bash
git add management-ui/src/layout/components/Sidebar.vue management-ui/tests/global-layout-responsiveness.test.js
git commit -m "fix: improve management sidebar readability"
```

### Task 4: Apply Stable Layouts to Order and List Pages

**Files:**
- Modify: `management-ui/src/views/function/price/price.vue:4-25,90-93`
- Modify: `management-ui/src/views/function/equipment/equipment.vue:4-76,365-375`
- Modify: `management-ui/src/views/function/order/order.vue:60-214,3144-3260,3664-3730`
- Review without business changes: inventory, approval, employee, organization, quality, customer, attendance, document, announcement, receipt, label and manual views.
- Test: `management-ui/tests/global-layout-responsiveness.test.js`

**Interfaces:**
- Consumes: shared classes from Task 2.
- Produces: stable 866px statistics, filters and table scroll for order, price and equipment pages.

- [ ] **Step 1: Repair order page density and responsiveness**

Keep the existing status and category data sources. Set `.order-summary-grid-new` to four columns on wide screens and two columns from 641px to 1280px. Set `.order-category-summary-grid-new` to four columns wide, two columns narrow, and one column compact. Add an `order-mobile-summary-toggle` button that is visible only below 640px and controls the secondary statistics area; default it closed on compact screens. Preserve click-only status selection.

Make `.order-filter-grid` use two columns from 641px to 1279px and twelve columns at 1280px. Add `white-space: nowrap` to the “创建时间” label and keep its two date inputs in a stable two-column nested grid. Reduce empty table minimum height to the height required by the empty state.

- [ ] **Step 2: Restructure price statistics and filters**

Give the statistics section `class="function-stats-grid"` and each `el-statistic` `class="function-stat-card"`. Add this shared card rule to `src/style.css`:

```css
.function-stat-card {
  min-width: 0;
  border: 1px solid rgba(200, 211, 223, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  padding: 1.25rem;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}
```

Give the price form `class="function-filter-form p-4"`. Add `aria-label` to keyword, status, batch, spec, currency, price range and effective-date controls. Wrap query/reset in `<div class="function-filter-actions">`. Wrap the price table in `<div class="function-table-scroll">`.

- [ ] **Step 3: Restructure equipment filters and table**

Change the header form class to `function-filter-form equipment-filter-form`. Add `aria-label="设备关键词"` and `aria-label="设备状态"`. Put query/reset/export/create buttons in one `.function-filter-actions` form item. Wrap `el-table` in `.function-table-scroll` while preserving its fixed operation column.

- [ ] **Step 4: Verify remaining grids inherit the corrected rule**

Run:

```powershell
rg -n "grid grid-cols-1.*md:grid-cols|grid-cols-2.*lg:grid-cols" src/views/function -g "*.vue"
```

Confirm the inventory, approval, employee, organization, bad-product, customer and attendance grids no longer match a global `!important` single-column override. Do not edit pages that already render correctly after Task 2.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/global-layout-responsiveness.test.js tests/element-plus-order.test.js tests/order-unified.test.js tests/element-plus-tenant-price.test.js tests/element-plus-organization-equipment.test.js`.

Expected: PASS.

- [ ] **Step 6: Commit page layout repairs**

```bash
git add management-ui/src/style.css management-ui/src/views/function/order/order.vue management-ui/src/views/function/price/price.vue management-ui/src/views/function/equipment/equipment.vue management-ui/tests/global-layout-responsiveness.test.js
git commit -m "fix: stabilize management list page layouts"
```

### Task 5: Replace the Collapsed Permission Selector with a Browsable List

**Files:**
- Create: `management-ui/src/views/function/role/permissionPresentation.js`
- Create: `management-ui/tests/permission-presentation.test.js`
- Modify: `management-ui/src/views/function/role/permissionDrawer.vue:1-220`

**Interfaces:**
- Produces: `permissionGroups(tree, keyword, selectedOnly, selectedIds)` and `groupLeafIds(group)`.
- Consumes: existing permission nodes `{ id, permName, permCode, children }` and selected numeric IDs.

- [ ] **Step 1: Write failing pure-function tests**

```js
import assert from 'node:assert/strict'
import test from 'node:test'
import { groupLeafIds, permissionGroups } from '../src/views/function/role/permissionPresentation.js'

const tree = [
  { id: 1, permName: '订单管理', children: [
    { id: 11, permName: '查看订单', permCode: 'order:list' },
    { id: 12, permName: '编辑订单', permCode: 'order:update' }
  ]},
  { id: 2, permName: '员工管理', children: [
    { id: 21, permName: '查看员工', permCode: 'employee:list' }
  ]}
]

test('permissionGroups keeps business groups and searchable leaves', () => {
  assert.deepEqual(permissionGroups(tree, '编辑', false, []), [
    { id: 1, name: '订单管理', permissions: [{ id: 12, name: '编辑订单', code: 'order:update' }] }
  ])
})

test('permissionGroups can show only selected leaves', () => {
  assert.deepEqual(permissionGroups(tree, '', true, [21])[0].permissions.map((item) => item.id), [21])
})

test('groupLeafIds returns unique numeric leaf ids', () => {
  assert.deepEqual(groupLeafIds(permissionGroups(tree, '', false, [])[0]), [11, 12])
})
```

- [ ] **Step 2: Run permission tests and verify RED**

Run: `node --test tests/permission-presentation.test.js`.

Expected: FAIL because `permissionPresentation.js` does not exist.

- [ ] **Step 3: Implement the presentation helpers**

```js
function leaves(nodes = []) {
  return nodes.flatMap((node) => node.children?.length ? leaves(node.children) : [{
    id: Number(node.id),
    name: node.permName || node.label || node.permCode || String(node.id),
    code: node.permCode || ''
  }])
}

export function permissionGroups(tree = [], keyword = '', selectedOnly = false, selectedIds = []) {
  const needle = String(keyword).trim().toLowerCase()
  const selected = new Set((selectedIds || []).map(Number))
  return tree.map((node) => {
    const permissions = leaves(node.children?.length ? node.children : [node]).filter((item) => {
      if (selectedOnly && !selected.has(item.id)) return false
      return !needle || `${item.name} ${item.code}`.toLowerCase().includes(needle)
    })
    return { id: Number(node.id), name: node.permName || node.label || '未分组', permissions }
  }).filter((group) => group.permissions.length)
}

export const groupLeafIds = (group) => [...new Set((group?.permissions || []).map((item) => Number(item.id)))]
```

- [ ] **Step 4: Replace `el-tree-select` with visible groups**

Add a labelled heading with `id="role-permission-title"`, set the drawer `aria-labelledby="role-permission-title"`, add search input `aria-label="搜索权限"`, “只看已选” switch, selected count, and a scrollable list of group cards. Each group contains a checkbox group bound to `checkedPermissionIds`; group header provides “全选本组/取消本组”. Keep existing loader, forbidden, failure, empty and save behavior unchanged.

- [ ] **Step 5: Run permission and existing role tests**

Run: `node --test tests/permission-presentation.test.js tests/permission-ui-hardening.test.js tests/element-plus-announcement-role.test.js`.

Expected: PASS.

- [ ] **Step 6: Commit permission UI repair**

```bash
git add management-ui/src/views/function/role/permissionPresentation.js management-ui/src/views/function/role/permissionDrawer.vue management-ui/tests/permission-presentation.test.js
git commit -m "fix: make role permissions browsable"
```

### Task 6: Build and Visual Regression

**Files:**
- Modify: `docs/audits/2026-07-17-page-layout/AUDIT.md`
- Create screenshots under: `docs/audits/2026-07-17-page-layout/fixed/`

**Interfaces:**
- Consumes: built management UI and authenticated local/online browser session.
- Produces: test evidence and screenshot comparison for all management pages.

- [ ] **Step 1: Run the complete management test suite**

Run: `npm test` from `management-ui`.

Expected: exit code 0 and no failed tests.

- [ ] **Step 2: Build the management frontend**

Run: `npm run build` from `management-ui`.

Expected: Vite exits 0 and writes `management-ui/dist`.

- [ ] **Step 3: Capture visual regressions at three widths**

Capture 1440px, 866px and 390px screenshots for dashboard, order, installation task, inventory, approval, employee, organization, role, quality, customer, price, attendance, equipment, document, announcement, receipt, label and manual. Verify:

- KPI grids use 4/2/1 columns where appropriate.
- Price and equipment filters retain readable widths.
- Tables scroll horizontally at 866px and become cards only at 390px where applicable.
- Desktop sidebar opens expanded and collapsed state is icon-only with tooltips.
- Role permissions are searchable and visible by group.
- No text overlap, cropped buttons or inaccessible dialog title is present.

- [ ] **Step 4: Update audit documentation**

Mark resolved findings with the new screenshot path and leave any unresolved finding explicitly open. Do not delete original before screenshots.

- [ ] **Step 5: Run final diff and build checks**

Run:

```bash
git diff --check
npm test
npm run build
```

Expected: all commands exit 0.

- [ ] **Step 6: Commit verification artifacts**

```bash
git add docs/audits/2026-07-17-page-layout management-ui
git commit -m "docs: record management layout verification"
```
