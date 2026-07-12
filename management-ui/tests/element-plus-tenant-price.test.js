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
