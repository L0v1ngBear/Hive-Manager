import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

const between = (source, start, end) => {
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end, startIndex);
  assert.ok(startIndex >= 0, `missing start marker: ${start}`);
  assert.ok(endIndex > startIndex, `missing end marker: ${end}`);
  return source.slice(startIndex, endIndex);
};

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

test("equipment renders one empty state through the ElTable empty slot", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const table = source.match(/<el-table\b[\s\S]*?<\/el-table>/)?.[0];

  assert.ok(table, "equipment table should exist");
  assert.match(table, /<template\s+#empty>[\s\S]*?<el-empty\b[\s\S]*?<\/template>/);
  assert.equal((table.match(/<el-empty\b/g) || []).length, 1);

  const afterTable = source.slice(source.indexOf(table) + table.length);
  const beforePagination = afterTable.slice(0, afterTable.indexOf('<div class="table-footer">'));
  assert.doesNotMatch(beforePagination, /<el-empty\b/);
});

test("equipment clears stale rows and persists distinct request failures with retry", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const fetchDevices = between(source, "async function fetchDevices()", "function handleSearch()");
  const clearIndex = fetchDevices.indexOf("devices.value = []");
  const requestIndex = fetchDevices.indexOf("getEquipmentPage(queryParams.value)");

  assert.ok(clearIndex >= 0 && clearIndex < requestIndex, "stale equipment rows must clear before requesting");
  assert.match(fetchDevices, /listFailure\.value = null/);
  assert.match(fetchDevices, /catch \(error\)[\s\S]*listFailure\.value = resolveRequestFailure\(error/);
  assert.match(source, /const listFailure = ref\(null\)/);
  assert.match(source, /v-if="listFailure"/);
  assert.match(source, /@click="fetchDevices"[^>]*>重试<\/el-button>/);
  assert.match(source, /function getRequestStatusCode\(error\)[\s\S]*error\?\.response\?\.status[\s\S]*error\?\.response\?\.data\?\.code[\s\S]*error\?\.statusCode[\s\S]*error\?\.code/);
  assert.match(source, /statusCode === 401/);
  assert.match(source, /statusCode === 403/);
  assert.match(source, /登录状态已失效/);
  assert.match(source, /暂无权限查看设备列表/);
  assert.match(source, /设备列表加载失败/);
});

test("organization clears members and only applies the latest department request", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const selectDepartment = between(source, "async function selectDepartment(node)", "function openCreate(parent)");
  const clearIndex = selectDepartment.indexOf("members.value = []");
  const requestIndex = selectDepartment.indexOf("getDepartmentEmployees(node.id)");

  assert.ok(clearIndex >= 0 && clearIndex < requestIndex, "old department members must clear before requesting");
  assert.match(source, /let memberRequestId = 0/);
  assert.match(selectDepartment, /const requestId = \+\+memberRequestId/);
  assert.match(selectDepartment, /if \(requestId !== memberRequestId\) return/);
  assert.match(selectDepartment, /catch \(error\)[\s\S]*memberFailure\.value = resolveMemberFailure\(error/);
  assert.match(selectDepartment, /finally[\s\S]*requestId === memberRequestId[\s\S]*memberLoading\.value = false/);
  assert.match(source, /const memberFailure = ref\(null\)/);
  assert.match(source, /v-if="memberFailure"/);
  assert.match(source, /@click="retryMembers"[^>]*>重试<\/el-button>/);
  assert.match(source, /function getRequestStatusCode\(error\)[\s\S]*error\?\.response\?\.status[\s\S]*error\?\.response\?\.data\?\.code[\s\S]*error\?\.statusCode[\s\S]*error\?\.code/);
  assert.match(source, /暂无权限查看部门成员/);
  assert.match(source, /部门成员加载失败/);
});

test("equipment explains its locked code and disables pagination while loading", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const pagination = source.match(/<el-pagination\b[\s\S]*?\/>/)?.[0] || "";

  assert.match(source, /<el-tooltip\b[\s\S]*设备码已用于固定二维码，创建后不可修改。[\s\S]*<el-input\b[\s\S]*:disabled="!!editingId"/);
  assert.match(pagination, /:disabled="loading"/);
});
