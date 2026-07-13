import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const tenant = readFileSync(
  new URL("../src/views/function/tenant/tenant.vue", import.meta.url),
  "utf8",
);
const price = readFileSync(
  new URL("../src/views/function/price/price.vue", import.meta.url),
  "utf8",
);
const priceCreate = readFileSync(
  new URL("../src/views/function/price/priceCreate.vue", import.meta.url),
  "utf8",
);
const priceDoc = readFileSync(new URL("../../docs/management-ui/modules/price.md", import.meta.url), "utf8");

function assertUsesComponents(source, names) {
  for (const name of names) {
    assert.match(source, new RegExp(`<${name}(?:\\s|>)`));
  }
}

function assertExplicitImports(source, names) {
  for (const name of names) {
    assert.match(source, new RegExp(`\\b${name}\\b`));
  }
  assert.match(source, /from ['"]element-plus['"]/);
}

test("migrates tenant controls to explicitly imported Element Plus components", () => {
  const components = [
    "el-table",
    "el-drawer",
    "el-form",
    "el-upload",
    "el-input-number",
    "el-date-picker",
    "el-checkbox-group",
    "el-switch",
  ];
  assertUsesComponents(tenant, components);
  assertExplicitImports(tenant, [
    "ElTable",
    "ElDrawer",
    "ElForm",
    "ElUpload",
    "ElInputNumber",
    "ElDatePicker",
    "ElCheckboxGroup",
    "ElSwitch",
  ]);
});

test("migrates price list and editor controls while retaining date string values", () => {
  const components = [
    "el-table",
    "el-pagination",
    "el-drawer",
    "el-form",
    "el-input-number",
    "el-date-picker",
    "el-select",
  ];
  assertUsesComponents(`${price}\n${priceCreate}`, components);
  assertExplicitImports(price, ["ElTable", "ElPagination", "ElDrawer", "ElForm"]);
  assertExplicitImports(priceCreate, ["ElInputNumber", "ElDatePicker", "ElSelect"]);
  assert.match(priceCreate, /value-format="YYYY-MM-DD"/);
  assert.match(priceCreate, /function formatLocalDate\(date = new Date\(\)\)/);
  assert.doesNotMatch(priceCreate, /toISOString\(\)\.slice\(0, 10\)/);
});

test("retains dynamic columns and the complete price detail business content", () => {
  assert.match(price, /useLocalTableColumns/);
  assert.match(price, /TableColumnSettings/);
  assert.match(price, /detail\?\.tierPrices/);
  assert.match(price, /detail\?\.overrides/);
  assert.match(price, /detail\?\.logs/);
  for (const field of ['createTime', 'remark', 'oldPrice', 'newPrice']) assert.match(price, new RegExp(field));
});

test("price commands use real permissions with disabled explanations and guards", () => {
  for (const permission of ["price:detail", "price:publish", "price:delete"]) {
    assert.match(price, new RegExp(permission.replace(":", "\\:")));
  }
  assert.match(price, /ElTooltip/);
  assert.match(price, /canViewDetail/);
  assert.match(price, /canPublish/);
  assert.match(price, /canAdjust/);
  assert.match(price, /canDelete/);
  assert.match(priceCreate, /canPublish/);
});

test("preserves zero numeric query and override values", () => {
  assert.doesNotMatch(price, /priceMin:\s*query\.priceMin\s*\|\|/);
  assert.doesNotMatch(price, /priceMax:\s*query\.priceMax\s*\|\|/);
  assert.doesNotMatch(priceCreate, /item\.customerId\s*&&\s*item\.price\)/);
  assert.match(priceCreate, /presentPriceOverrides\(form\.overrides\)/);
});

test("models mutually exclusive request states, retries, request ids, and pending commands", () => {
  for (const source of [price, tenant]) {
    assert.match(source, /requestError/);
    assert.match(source, /retry/);
  }
  assert.match(tenant, /featureError/);
  assert.match(price, /detailRequestId/);
  assert.match(priceCreate, /detailRequestId/);
  assert.match(tenant, /createLatestLoadingController/);
  assert.match(tenant, /logoRequest\.finish\(requestId\)/);
  assert.match(tenant, /statusPending/);
  for (const pending of ["deletingId", "importing", "exporting", "downloadingTemplate"]) {
    assert.match(price, new RegExp(pending));
  }
});

test("price module record describes the completed local-date fix instead of a stale UTC risk", () => {
  assert.doesNotMatch(priceDoc, /新建默认生效日期使用 `new Date\(\)\.toISOString\(\)\.slice\(0, 10\)`/);
  assert.match(priceDoc, /formatLocalDate/);
  assert.match(priceDoc, /本地 `YYYY-MM-DD`/);
});
