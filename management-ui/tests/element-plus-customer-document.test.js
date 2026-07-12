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

test("customer and document current-page exports use explicit row data", () => {
  const customer = read("../src/views/function/customer/customer.vue");
  const document = read("../src/views/function/document/document.vue");
  const settings = read("../src/components/TableColumnSettings.vue");

  for (const [source, rows, cell] of [
    [customer, "customerList", "customerExportCell"],
    [document, "filteredDocumentList", "documentExportCell"],
  ]) {
    assert.match(source, new RegExp(`:export-rows="${rows}"`));
    assert.match(source, new RegExp(`:export-cell="${cell}"`));
    assert.doesNotMatch(source, /exportTableElementToExcel|findExportTable\(|querySelector(?:All)?\(/);
  }

  assert.match(settings, /exportRowsToExcel/);
  assert.match(
    settings,
    /if \(hasStructuredExport\(\)\) \{[\s\S]*?await exportRowsToExcel\([\s\S]*?return[\s\S]*?\}\s*await exportTableElementToExcel\(findExportTable\(\)/,
  );
});

test("structured current-page export retains the row limit", () => {
  const settings = read("../src/components/TableColumnSettings.vue");

  assert.match(settings, /const MAX_CURRENT_PAGE_ROWS = 2000/);
  assert.match(
    settings,
    /if \(props\.exportRows\.length > MAX_CURRENT_PAGE_ROWS\) \{[\s\S]*?throw new Error/,
  );
});
