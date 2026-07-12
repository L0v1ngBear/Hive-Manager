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
  assert.match(source, /const DepartmentNode\s*=\s*defineComponent/);
});

test("organization clears an Element Plus parent selector back to the root value", () => {
  const source = read("../src/views/function/organization/organization.vue");
  assert.match(source, /:value-on-clear="''"/);
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
