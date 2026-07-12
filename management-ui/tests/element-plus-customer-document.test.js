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
