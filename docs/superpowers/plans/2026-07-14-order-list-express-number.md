# Order List Express Number Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the management order list's remark column with the order form's logistics tracking number.

**Architecture:** Reuse the existing `expressNo` field already returned by the order page API. Change only the order table presentation, local column configuration, export mapping, and their regression test; preserve the order form's `remark` field and backend contracts.

**Tech Stack:** Vue 3, JavaScript, Node.js built-in test runner, Vite

## Global Constraints

- Keep the order edit form's `remark` field unchanged.
- Show only `informationChannel` in the information-channel list column.
- Use `expressNo` as the logistics tracking number field.
- Do not change backend DTOs, database schema, or APIs.

---

### Task 1: Replace the Order List Remark Column

**Files:**
- Modify: `management-ui/tests/order-information-channel-and-advance.test.js`
- Modify: `management-ui/src/views/function/order/order.vue`

**Interfaces:**
- Consumes: `SalesOrderPageVO.expressNo` exposed as `row.expressNo`.
- Produces: an `expressNo` table column labeled `物流单号`, and the same field in order-list exports.

- [x] **Step 1: Write the failing regression test**

Add assertions that the default table columns contain `{key: 'expressNo', label: '物流单号'}` and no list column keyed by `remark`; the list cell renders `row.expressNo`; the information-channel cell no longer renders `row.expressCompany` or `row.expressNo`; and export formatting handles `expressNo` directly.

- [x] **Step 2: Run the test to verify it fails**

Run: `node --test tests/order-information-channel-and-advance.test.js`

Expected: FAIL because the current list still defines and renders the `remark` column.

- [x] **Step 3: Implement the minimal presentation change**

In `order.vue`:

```js
{key: 'informationChannel', label: '信息渠道'},
{key: 'expressNo', label: '物流单号'},
```

Render `row.expressNo || '未填写物流单号'`, remove logistics data from the information-channel cell, map `expressNo` in `formatOrderExportCell`, rename the column CSS selector to `order-column-expressNo`, and change local column storage from `order.list.commercial.v3` to `order.list.commercial.v4`.

- [x] **Step 4: Run focused and related tests**

Run: `node --test tests/order-information-channel-and-advance.test.js tests/order-permission-hardening.test.js tests/element-plus-component-imports.test.js`

Expected: all tests PASS.

- [x] **Step 5: Build and stage the deployment artifact**

Run: `npm run build`

Expected: Vite build exits with code 0. Mirror `management-ui/dist` to `C:/Users/HUAWEI/Desktop/hive部署_全新配置/management-ui/dist` and refresh `RELEASE_BUILD_INFO.txt` with the new frontend hash and file count.
