import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const globalStyle = read("../src/style.css");

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

test("customer list separates header actions from collapsible filters", () => {
  const customer = read("../src/views/function/customer/customer.vue");
  const header = customer.match(/<header class="customer-page-header[\s\S]*?<\/header>/)?.[0] || "";
  const listPanel = customer.match(/<section class="customer-list-panel[\s\S]*?<CustomerCreateDrawer/)?.[0] || "";

  assert.match(header, /@click="openCreateDrawer"/);
  assert.doesNotMatch(header, /v-filter-collapse|class="function-filter-form/);
  assert.match(listPanel, /v-filter-collapse class="function-filter-form customer-filter-form"/);
  assert.match(listPanel, /class="function-table-scroll responsive-table-wrap"/);
  assert.match(listPanel, /<el-table-column label="操作" fixed="right" width="128"/);
  assert.match(customer, /class="customer-summary-grid"/);
  assert.match(customer, /\.customer-filter-form\s*\{[\s\S]*grid-template-columns/);
  assert.match(customer, /@container \(max-width: 64rem\)[\s\S]*\.customer-filter-form/);
  assert.match(customer, /@container \(max-width: 40rem\)[\s\S]*\.customer-filter-form/);
});

test("customer filters use shell-width container queries at desktop sidebar widths", () => {
  const customer = read("../src/views/function/customer/customer.vue");
  const rem = 16;
  const columnsAt = (shellWidth) => {
    if (shellWidth <= 40 * rem) return 1;
    if (shellWidth <= 64 * rem) return 2;
    return 5;
  };

  assert.match(globalStyle, /\.function-page-shell\s*\{[\s\S]*?container-type\s*:\s*inline-size/);
  assert.match(customer, /@container \(max-width: 64rem\)\s*\{[\s\S]*?\.customer-filter-form\s*\{[\s\S]*?grid-template-columns\s*:\s*repeat\(2, minmax\(0, 1fr\)\)/);
  assert.match(customer, /@container \(max-width: 40rem\)\s*\{[\s\S]*?\.customer-filter-form\s*\{[\s\S]*?grid-template-columns\s*:\s*minmax\(0, 1fr\)/);
  assert.doesNotMatch(customer, /@media \(max-width: (?:900|640)px\)[\s\S]*?\.customer-filter-form/);

  assert.equal(columnsAt(1024 - 256), 2, "1024px viewport with expanded sidebar uses two tracks");
  assert.equal(columnsAt(1024 - 88), 2, "1024px viewport with collapsed sidebar uses two tracks");
  assert.equal(columnsAt(390), 1, "compact content container uses one track");
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

  for (const permission of ["customer:create", "customer:update", "customer:detail", "customer:export"]) {
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

  for (const permission of ["document:folder:create", "document:file:upload", "document:export", "document:move", "document:delete"]) {
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

  assert.match(document, /hasPermission\('document:list'\)/);
  assert.match(document, /:disabled="currentParentId === 0 \|\| !canBrowseDocuments"/);
  assert.match(document, /:disabled="!canBrowseDocuments"/);
  assert.match(document, /breadcrumbPermissionReason/);
  assert.match(document, /documentNavigator\.openFolder/);
  assert.equal((document.match(/:disabled="!canBrowseDocuments"/g) || []).length, 3);
  assert.equal((document.match(/:title="canBrowseDocuments \?/g) || []).length, 4);
  assert.match(document, /documentNavigator\.goRoot\(\)/);
});

test("document center supports 200MB chunk completion, office archives, move and delete", () => {
  const document = read("../src/views/function/document/document.vue");
  const api = read("../src/views/function/document/api/document.js");
  const chunkedUpload = read("../src/utils/chunkedAttachmentUpload.js");

  for (const extension of [".ppt", ".pptx", ".rar"]) {
    assert.ok(document.includes(extension), `document picker must accept ${extension}`);
  }
  assert.match(document, /uploadAttachmentWithChunks\(file,[\s\S]*?'document'/);
  assert.match(document, /completeChunkedDocumentUpload\(uploadId, uploadParentId\)/);
  assert.match(chunkedUpload, /typeof options\.complete === 'function'/);
  assert.match(api, /\/document\/file\/chunked\/\$\{uploadId\}\/complete/);
  assert.match(api, /url: '\/document\/move'/);
  assert.match(api, /url: `\/document\/\$\{documentId\}`[\s\S]*?method: 'delete'/);
  assert.match(document, /label="操作"[\s\S]*?>[\s\S]*?移动[\s\S]*?删除/);
  assert.match(document, /ElMessageBox\.confirm/);
  assert.match(document, /buildMoveFolderOptions/);
});
