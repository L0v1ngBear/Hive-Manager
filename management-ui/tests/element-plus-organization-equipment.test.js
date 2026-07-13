import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import { buildEquipmentExport } from "../src/views/function/equipment/equipmentExport.js";

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

test("equipment export serializes the current page rows without table DOM clones", () => {
  const devices = [{ id: 7, equipmentName: "定型机01", equipmentCode: "EQ-001", equipmentType: "生产设备", location: "一车间", responsiblePerson: "张三", inspectionCycleDays: 0, lastInspectionTime: "2026-07-13T08:09:00", status: "enabled" }];
  const result = buildEquipmentExport(devices);
  assert.deepEqual(result.headers, ["设备名称", "设备编码", "设备类型", "设备位置", "负责人", "巡检周期（天）", "最近巡检", "状态"]);
  assert.deepEqual(result.rows, [["定型机01", "EQ-001", "生产设备", "一车间", "张三", 0, "2026-07-13 08:09", "启用中"]]);
  assert.equal(result.rows.length, devices.length, "fixed 操作列克隆不能产生重复导出行");
});

test("organization overview has mutually exclusive failure and retry state", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const fetchOverview = between(source, "async function fetchOverview()", "async function selectDepartment(node)");
  assert.match(source, /const overviewFailure = ref\(null\)/);
  assert.match(fetchOverview, /departments\.value = \[\][\s\S]*overviewFailure\.value = null[\s\S]*getOrganizationOverview/);
  assert.match(fetchOverview, /catch \(error\)[\s\S]*overviewFailure\.value = resolveOverviewFailure\(error/);
  assert.match(source, /v-if="overviewFailure"[\s\S]*@click="fetchOverview"[^>]*>重试<\/el-button>/);
});

test("equipment detail and records reject stale responses and expose retryable states", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const openDetail = between(source, "async function openDetail(device)", "async function fetchRecords(");
  const fetchRecords = between(source, "async function fetchRecords(", "function closeDrawers()");
  assert.match(source, /let detailRequestId = 0/);
  assert.match(openDetail, /detail\.value = null[\s\S]*records\.value = \[\][\s\S]*const requestId = \+\+detailRequestId/);
  assert.match(openDetail, /if \(requestId !== detailRequestId\) return/);
  assert.match(fetchRecords, /const requestId = \+\+recordsRequestId[\s\S]*if \(requestId !== recordsRequestId\) return/);
  assert.match(source, /detailFailure/);
  assert.match(source, /recordsFailure/);
  assert.match(source, /retryDetail/);
  assert.match(source, /retryRecords/);
});

test("organization and equipment mutation commands stay visible but disabled with reasons", () => {
  const organization = read("../src/views/function/organization/organization.vue");
  const equipment = read("../src/views/function/equipment/equipment.vue");
  assert.match(organization, /hasPermission\('employee:update'\)/);
  assert.match(organization, /hasPermission\('employee:delete'\)/);
  assert.match(organization, /暂无 employee:update 权限/);
  assert.match(organization, /暂无 employee:delete 权限/);
  assert.match(equipment, /hasPermission\('equipment:save'\)/);
  assert.match(equipment, /暂无 equipment:save 权限/);
});

test("organization and equipment module records describe the migrated behavior consistently", () => {
  const organization = read("../../docs/management-ui/modules/organization.md");
  const equipment = read("../../docs/management-ui/modules/equipment.md");

  for (const staleStatement of [
    "overview 和成员请求没有本地 `catch`",
    "选择新部门前不会先清空旧 `members`",
    "已确认的成员请求竞态",
    "变更按钮无前端权限门",
  ]) {
    assert.doesNotMatch(organization, new RegExp(staleStatement));
  }
  for (const staleStatement of [
    "从 `ElTable` 的真实 DOM 根节点读取",
    "页面缺少 `equipment:save` 的命令级禁用状态",
    "当前导出绑定真实 DOM 表格",
    "页面没有独立错误占位或重试面板",
  ]) {
    assert.doesNotMatch(equipment, new RegExp(staleStatement));
  }
});
