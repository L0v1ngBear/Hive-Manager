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
  assert.match(settings, /buildStructuredExportData/);
  assert.match(settings, /await exportRowsToExcel\([\s\S]*?headers: exportData\.headers,[\s\S]*?rows: exportData\.rows/);
});

test("structured current-page export retains the row limit", () => {
  const settings = read("../src/components/TableColumnSettings.vue");

  assert.match(settings, /const MAX_CURRENT_PAGE_ROWS = 2000/);
  assert.match(
    settings,
    /if \(props\.exportRows\.length > MAX_CURRENT_PAGE_ROWS\) \{[\s\S]*?throw new Error/,
  );
});

test("customer editor prevents native submit and keeps footer buttons non-submit", () => {
  const editor = read("../src/views/function/customer/customerCreate.vue");

  assert.match(editor, /<el-form\b[\s\S]*?@submit\.prevent="submit"/);
  assert.match(
    editor,
    /<el-button\b(?=[^>]*native-type="button")(?=[^>]*@click="closeDrawer")[^>]*>/,
  );
  assert.match(
    editor,
    /<el-button\b(?=[^>]*native-type="button")(?=[^>]*@click="submit")[^>]*>/,
  );
});

test("customer and document lists expose mutually exclusive persistent load states", () => {
  const customer = read("../src/views/function/customer/customer.vue");
  const document = read("../src/views/function/document/document.vue");

  assert.match(customer, /const listError = ref\(null\)/);
  assert.match(customer, /onLoading\(value\)[\s\S]*?customerList\.value = \[\]/);
  assert.match(customer, /customerListRunner\.run\(\(\) => getCustomerPage/);
  assert.match(customer, /status === 401[\s\S]*?status === 403[\s\S]*?status >= 500/);
  assert.match(customer, /<el-result\b[\s\S]*?v-if="listError"[\s\S]*?@click="fetchCustomerList"/);

  assert.match(document, /const documentError = ref\(null\)/);
  assert.match(document, /documentList\.value = \[\][\s\S]*?await getDocumentList/);
  assert.match(document, /status === 401[\s\S]*?status === 403[\s\S]*?status >= 500/);
  assert.match(document, /<el-result\b[\s\S]*?v-if="documentError"[\s\S]*?@click="retryDocuments"/);
  assert.match(document, /const hasDocumentFilters = computed/);
  assert.match(document, /const documentEmptyDescription = computed/);
  assert.match(document, /当前目录为空/);
  assert.match(document, /没有符合筛选条件的文档/);
});

test("customer and document commands keep visible disabled permission tooltips", () => {
  const customer = read("../src/views/function/customer/customer.vue");
  const editor = read("../src/views/function/customer/customerCreate.vue");
  const document = read("../src/views/function/document/document.vue");
  const settings = read("../src/components/TableColumnSettings.vue");
  const upload = read("../src/components/DragAttachmentUpload.vue");

  for (const permission of ["customer:add", "customer:update", "customer:detail", "table:export"]) {
    assert.match(`${customer}\n${editor}`, new RegExp(permission));
  }
  assert.match(customer, /:disabled="!canCreateCustomer"/);
  assert.match(customer, /:disabled="!canUpdateCustomer"/);
  assert.match(customer, /:disabled="!canViewCustomerDetail"/);
  assert.match(customer, /:export-disabled="!canExportTable"/);
  assert.match(customer, /当前账号暂无新增客户权限/);
  assert.match(customer, /当前账号暂无编辑客户权限/);
  assert.match(customer, /当前账号暂无查看客户详情权限/);
  assert.match(customer, /当前账号暂无表格导出权限/);

  for (const permission of ["document:folder:create", "document:file:upload", "table:export"]) {
    assert.match(document, new RegExp(permission));
  }
  assert.match(document, /:disabled="!canCreateFolder"/);
  assert.match(document, /:disabled="!canUploadDocument"/);
  assert.match(document, /:export-disabled="!canExportTable"/);
  assert.match(document, /当前账号暂无新建文件夹权限/);
  assert.match(document, /当前账号暂无上传文档权限/);
  assert.match(document, /当前账号暂无表格导出权限/);

  assert.match(settings, /exportDisabled/);
  assert.match(settings, /exportDisabledReason/);
  assert.match(settings, /:disabled="exportDisabled"/);
  assert.match(settings, /:title="exportDisabledReason/);
  assert.match(upload, /disabledReason/);
  assert.match(upload, /'is-disabled': disabled/);
  assert.match(upload, /props\.uploading \|\| props\.disabled/);
});

test("document directory requests ignore stale responses", () => {
  const document = read("../src/views/function/document/document.vue");

  assert.match(document, /let documentRequestId = 0/);
  assert.match(document, /const requestId = \+\+documentRequestId/);
  assert.match(document, /if \(requestId !== documentRequestId\) return/);
});

test("customer requests ignore stale responses and detail exposes retryable states", () => {
  const customer = read("../src/views/function/customer/customer.vue");

  assert.match(customer, /createLatestRequestRunner/);
  assert.match(customer, /customerListRunner\.run/);
  assert.match(customer, /customerDetailRunner\.run/);
  assert.match(customer, /@close="invalidateCustomerDetail"/);
  assert.match(customer, /v-else-if="detailError"/);
  assert.match(customer, /@click="retryCustomerDetail"/);
  assert.match(customer, /v-else-if="detailEmpty"/);
});

test("document navigation keeps breadcrumb permission commands visible and disabled", () => {
  const document = read("../src/views/function/document/document.vue");

  assert.match(document, /hasPermission\('document:breadcrumbs'\)/);
  assert.match(document, /:disabled="currentParentId === 0 \|\| !canBrowseDocuments"/);
  assert.match(document, /:disabled="!canBrowseDocuments"/);
  assert.match(document, /breadcrumbPermissionReason/);
  assert.match(document, /documentNavigator\.openFolder/);
});
